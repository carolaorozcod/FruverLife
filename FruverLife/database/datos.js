db = db.getSiblingDB("inventario_db");

// ===================== SEGURIDAD =====================
// El script borra datos de prueba antes de validar. Haz primero un respaldo:
//   mongodump --db inventario_db --out respaldo_inventario
// y luego cambia esta variable a true.
var RESPALDO_HECHO = true;
if (!RESPALDO_HECHO) {
    throw new Error("Haz un mongodump de inventario_db y cambia RESPALDO_HECHO a true.");
}

// ===================== CONFIGURACION =====================
var PKG = "com.FruverLifes.Model.";            // paquete exacto de tus clases Java
var TIPO_ACUMULACION = "ACUMULACION";
var TIPO_REDENCION_DEFECTO = "REDENCION";       // se usa solo si no hay otro tipo en la base
var ESTADO_ACTIVO = "Activo";                   // estado de proveedores
var ESTADO_INACTIVO = "Inactivo";
var ESTADO_PRODUCTO = "ACTIVO";                 // estado de productos
// Usuarios (accesoSistema.usuario) que atienden como cajeros en las ventas generadas
var CAJEROS_USUARIOS = ["kevin.admin", "cristian."];
// Descuento a clientes que tambien son usuarios activos del sistema (igual que en la app)
var PORCENTAJE_DESCUENTO_USUARIO = 0.25;

// ===================== METAS DEL PROYECTO =====================
var META = { clientes: 50, proveedores: 50, productos: 140, compras: 5360,
             ventas: 8000, desperdicio: 1895, movimientos: 9500 };
var inicio = new Date(Date.UTC(2026, 8, 1));   // 1 de septiembre de 2026
var diasTotales = 75;                          // hasta el 14 de noviembre
var diasAgotado = 60;                          // los agotados dejan de moverse 15 dias antes
var PCT_SIN_STOCK = 0.08;
var PCT_AGOTADO = 0.12;
var MAX_CANT_VENTA = 3;
var DIA = 86400000;

function rnd(a, b) { return a + Math.random() * (b - a); }
function rint(a, b) { return a + Math.floor(Math.random() * (b - a + 1)); }
function elegir(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
function mezclar(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
        let j = Math.floor(Math.random() * (i + 1));
        let t = arr[i]; arr[i] = arr[j]; arr[j] = t;
    }
    return arr;
}
function hex(o) { return o.toHexString(); }
function pad(n, l) { return String(n).padStart(l, "0"); }
function insertar(col, docs) {
    for (let i = 0; i < docs.length; i += 1000) db[col].insertMany(docs.slice(i, i + 1000));
    print("  " + col + ": " + docs.length + " insertados");
}

// ===================== 1) LIMPIAR DATOS DE PRUEBA ANTERIORES =====================
print("--- 1) Limpiando datos de prueba anteriores ---");
db.productos.find({ ajusteStockPrueba: { $exists: true } }).toArray().forEach(p => {
    var upd = { $inc: { cantidad: -p.ajusteStockPrueba }, $unset: { ajusteStockPrueba: "" } };
    if (p.precioCompraDePrueba) { upd.$unset.precioCompra = ""; upd.$unset.precioCompraDePrueba = ""; }
    db.productos.updateOne({ _id: p._id }, upd);
});
db.productos.deleteMany({ esDePrueba: true });
db.personas.find({ ajustePuntosPrueba: { $exists: true } }).toArray().forEach(p => {
    db.personas.updateOne({ _id: p._id }, { $inc: { puntos: -p.ajustePuntosPrueba }, $unset: { ajustePuntosPrueba: "" } });
});
["compras", "ventas", "movimientosPuntos", "registrosDesperdicio"].forEach(c => {
    print("  " + c + " de prueba borradas: " + db[c].deleteMany({ esDePrueba: true }).deletedCount);
});

// ===================== 2) PERSONAS =====================
print("--- 2) Personas ---");
function recortar(funcion, meta) {
    var lista = db.personas.find({ funciones: funcion }).toArray();
    var exceso = lista.length - meta;
    if (exceso <= 0) return;
    var generadas = lista.filter(p => /^1000\d{6}$/.test(p.identificacion || ""))
        .sort((a, b) => b.identificacion.localeCompare(a.identificacion));
    var borrar = generadas.slice(0, exceso).map(p => p._id);
    db.personas.deleteMany({ _id: { $in: borrar } });
    print("  " + funcion + ": se quitaron " + borrar.length + " personas de prueba sobrantes");
}
recortar("CLIENTE", META.clientes);
recortar("PROVEEDOR", META.proveedores);

// Estado de los proveedores generados que no lo tienen (aprox. 20% inactivos)
db.personas.find({ funciones: "PROVEEDOR", identificacion: /^1000\d{6}$/ }).toArray()
    .sort((a, b) => a.identificacion.localeCompare(b.identificacion))
    .forEach((p, i) => {
        if (!p.datosProveedor || !p.datosProveedor.estado) {
            db.personas.updateOne({ _id: p._id },
                { $set: { "datosProveedor.estado": (i % 5 === 4) ? ESTADO_INACTIVO : ESTADO_ACTIVO } });
        }
    });

var proveedores = db.personas.find({ funciones: "PROVEEDOR" }).toArray();
var activos = proveedores.filter(p => p.datosProveedor &&
    String(p.datosProveedor.estado).toLowerCase() === ESTADO_ACTIVO.toLowerCase());
var activosSet = new Set(activos.map(p => hex(p._id)));
var clientes = db.personas.find({ funciones: "CLIENTE" }).toArray();
var usuarios = db.personas.find({ "accesoSistema.habilitado": true }).toArray();
var rolesDe = p => (p.accesoSistema && p.accesoSistema.roles) || [];
var compradores = usuarios.filter(u => rolesDe(u).some(r => /gerente|administrador/i.test(r)));
var admins = usuarios.filter(u => rolesDe(u).some(r => /administrador/i.test(r)));
var cajerosPermitidos = CAJEROS_USUARIOS.map(x => x.toLowerCase().trim());
var cajeros = usuarios.filter(u => cajerosPermitidos.indexOf(
    String((u.accesoSistema && u.accesoSistema.usuario) || "").toLowerCase().trim()) >= 0);

if (clientes.length === 0 || usuarios.length === 0 || compradores.length === 0 || admins.length === 0 ||
    cajeros.length === 0 || activos.length < 3) {
    throw new Error("Faltan datos: clientes=" + clientes.length + ", usuarios=" + usuarios.length +
        ", compradores=" + compradores.length + ", administradores=" + admins.length +
        ", cajeros=" + cajeros.length + " (buscando " + CAJEROS_USUARIOS.join(", ") + ")" +
        ", proveedores activos=" + activos.length);
}
if (cajeros.length !== CAJEROS_USUARIOS.length) {
    print("  AVISO: se esperaban " + CAJEROS_USUARIOS.length + " cajeros y se encontraron " + cajeros.length);
}
print("  Clientes: " + clientes.length + " | Proveedores: " + proveedores.length +
    " (activos: " + activos.length + ") | Usuarios: " + usuarios.length);

// ===================== 3) PRODUCTOS =====================
print("--- 3) Productos ---");

// 3a) Catalogo base (PRD-006 a PRD-050): precios deterministas, idempotente por codigo
var nombresFrutas = ["Manzana", "Banano", "Pera", "Fresa", "Mango", "Papaya", "Sandía", "Melón", "Piña", "Uva", "Mandarina", "Naranja", "Durazno", "Kiwi", "Ciruela", "Maracuyá", "Guayaba", "Lulo", "Mora", "Tomate de árbol", "Frambuesa", "Arándano", "Cereza", "Limón", "Granadilla"];
var nombresVerduras = ["Lechuga", "Zanahoria", "Brócoli", "Espinaca", "Cebolla", "Ajo", "Pepino", "Apio", "Tomate", "Papa", "Yuca", "Plátano", "Remolacha", "Repollo", "Calabacín", "Berenjena", "Pimentón", "Cilantro", "Perejil", "Habichuela", "Arveja", "Coliflor", "Rábano", "Acelga", "Calabaza"];
var unidadesBase = ["kg", "libra", "unidad"];
var nombresPrev = new Set(db.productos.find({}, { nombre: 1 }).toArray().map(d => String(d.nombre).toLowerCase().trim()));
var opsBase = [];
for (let i = 6; i <= 50; i++) {
    let esFruta = (i % 2 === 0);
    let nombreBase = esFruta ? nombresFrutas[(i - 1) % nombresFrutas.length] : nombresVerduras[(i - 1) % nombresVerduras.length];
    if (nombresPrev.has(nombreBase.toLowerCase())) continue;
    nombresPrev.add(nombreBase.toLowerCase());
    opsBase.push({ updateOne: {
        filter: { codigo: "PRD-" + pad(i, 3) },
        update: { $setOnInsert: {
            codigo: "PRD-" + pad(i, 3),
            nombre: nombreBase,
            categoria: esFruta ? "Frutas" : "Verduras",
            precioVenta: 3500 + (i * 35),
            cantidad: NumberInt(0),
            unidadMedida: unidadesBase[i % unidadesBase.length],
            estado: ESTADO_PRODUCTO,
            esDePrueba: true,
            _class: PKG + "Producto"
        } },
        upsert: true
    } });
}
if (opsBase.length) {
    var resBase = db.productos.bulkWrite(opsBase);
    print("  Catalogo base -> insertados: " + resBase.upsertedCount + " | ya existian: " + resBase.matchedCount);
}

// 3b) Resto de productos hasta la meta
var candidatos = [
 ["Aguacate Hass","Frutas"],["Aguacate Papelillo","Frutas"],["Anón","Frutas"],["Badea","Frutas"],["Banano","Frutas"],
 ["Banano Bocadillo","Frutas"],["Banano Criollo","Frutas"],["Borojó","Frutas"],["Breva","Frutas"],["Carambolo","Frutas"],
 ["Chirimoya","Frutas"],["Coco","Frutas"],["Curuba","Frutas"],["Feijoa","Frutas"],["Fresa","Frutas"],
 ["Granada","Frutas"],["Guanábana","Frutas"],["Guayaba Pera","Frutas"],["Higo","Frutas"],["Lima","Frutas"],
 ["Limón Tahití","Frutas"],["Limón Mandarino","Frutas"],["Mamoncillo","Frutas"],["Mamey","Frutas"],["Mandarina Oneco","Frutas"],
 ["Mandarina Arrayana","Frutas"],["Mango Tommy","Frutas"],["Mango de Azúcar","Frutas"],["Mango Kent","Frutas"],["Manzana Verde","Frutas"],
 ["Manzana Gala","Frutas"],["Manzana Fuji","Frutas"],["Maracuyá Amarillo","Frutas"],["Melocotón","Frutas"],["Mora de Castilla","Frutas"],
 ["Naranja Valencia","Frutas"],["Naranja Ombligona","Frutas"],["Nectarina","Frutas"],["Níspero","Frutas"],["Papaya Maradol","Frutas"],
 ["Papaya Hawaiana","Frutas"],["Papayuela","Frutas"],["Pera Williams","Frutas"],["Pera Packham","Frutas"],["Piña Oro Miel","Frutas"],
 ["Piña Perolera","Frutas"],["Pitahaya Amarilla","Frutas"],["Pitahaya Roja","Frutas"],["Pomelo","Frutas"],["Sandía Baby","Frutas"],
 ["Melón Cantaloupe","Frutas"],["Tamarindo","Frutas"],["Uchuva","Frutas"],["Uva Isabella","Frutas"],["Uva Roja","Frutas"],
 ["Uva Verde sin Semilla","Frutas"],["Zapote","Frutas"],["Tangelo","Frutas"],["Toronja","Frutas"],["Ciruela Roja","Frutas"],
 ["Durazno Criollo","Frutas"],["Lulo de Castilla","Frutas"],["Tomate de Árbol Rojo","Frutas"],["Granadilla Amarilla","Frutas"],["Cereza Importada","Frutas"],
 ["Frambuesa Importada","Frutas"],["Arándano Importado","Frutas"],["Kiwi Dorado","Frutas"],
 // --- nombres extra de frutas (margen de seguridad) ---
 ["Caimito","Frutas"],["Gulupa","Frutas"],["Lichi","Frutas"],["Mangostino","Frutas"],["Ciruela Verde","Frutas"],
 ["Mora Andina","Frutas"],["Arazá","Frutas"],["Tomate de Árbol Amarillo","Frutas"],["Mandarina Común","Frutas"],["Naranja Sanguina","Frutas"],
 ["Limón Pajarito","Frutas"],["Guayaba Agria","Frutas"],["Papaya Tainung","Frutas"],["Uva Negra","Frutas"],["Piña Gold","Frutas"],
 ["Acelga Roja","Verduras"],["Ahuyama","Verduras"],["Ají Dulce","Verduras"],["Ají Picante","Verduras"],["Albahaca","Verduras"],
 ["Alcachofa","Verduras"],["Apio España","Verduras"],["Arracacha","Verduras"],["Arveja Verde","Verduras"],["Batata","Verduras"],
 ["Berenjena Larga","Verduras"],["Brócoli","Verduras"],["Calabacín Amarillo","Verduras"],["Cebolla Larga","Verduras"],["Cebolla Cabezona Roja","Verduras"],
 ["Cebolla Cabezona Blanca","Verduras"],["Cebolla Puerro","Verduras"],["Cebollín","Verduras"],["Champiñón","Verduras"],["Cilantro Crespo","Verduras"],
 ["Col Morada","Verduras"],["Coliflor Morada","Verduras"],["Cubios","Verduras"],["Espárrago","Verduras"],["Espinaca Baby","Verduras"],
 ["Fríjol Verde","Verduras"],["Haba Verde","Verduras"],["Hierbabuena","Verduras"],["Hinojo","Verduras"],["Jengibre","Verduras"],
 ["Lechuga Crespa","Verduras"],["Lechuga Batavia","Verduras"],["Lechuga Romana","Verduras"],["Lechuga Morada","Verduras"],["Maíz Tierno","Verduras"],
 ["Menta","Verduras"],["Nabo","Verduras"],["Ñame","Verduras"],["Orégano","Verduras"],["Papa Criolla","Verduras"],
 ["Papa Pastusa","Verduras"],["Papa Sabanera","Verduras"],["Papa Capira","Verduras"],["Papa Rubí","Verduras"],["Pepino Cohombro","Verduras"],
 ["Pimentón Rojo","Verduras"],["Pimentón Verde","Verduras"],["Pimentón Amarillo","Verduras"],["Rábano Rojo","Verduras"],["Remolacha Baby","Verduras"],
 ["Repollo Blanco","Verduras"],["Repollo Morado","Verduras"],["Romero","Verduras"],["Tomate Cherry","Verduras"],["Tomate Chonto","Verduras"],
 ["Tomate Larga Vida","Verduras"],["Tomate Riñón","Verduras"],["Tomillo","Verduras"],["Zanahoria Baby","Verduras"],["Zapallo","Verduras"],
 ["Plátano Hartón","Verduras"],["Plátano Maduro","Verduras"],["Plátano Verde","Verduras"],["Rúgula","Verduras"],["Kale","Verduras"],
 ["Perejil Crespo","Verduras"],["Cebolla Junca","Verduras"],["Ajo Pelado","Verduras"],["Habichuela Larga","Verduras"],
 // --- nombres extra de verduras (margen de seguridad) ---
 ["Apio Nabo","Verduras"],["Col China","Verduras"],["Bok Choy","Verduras"],["Rábano Blanco","Verduras"],["Pepino Dulce","Verduras"],
 ["Lechuga Lollo Rosso","Verduras"],["Fríjol Cargamanto","Verduras"],["Arveja en Vaina","Verduras"],["Perejil Liso","Verduras"],["Yuca","Verduras"]
];
var existentes = db.productos.find().toArray();
var nombresUsados = new Set(existentes.map(p => String(p.nombre).toLowerCase().trim()));
var maxNum = 0;
existentes.forEach(p => { var n = parseInt(String(p.codigo).replace(/\D/g, "")) || 0; if (n > maxNum) maxNum = n; });
var faltan = META.productos - existentes.length;
if (faltan > 0) {
    var elegidos = mezclar(candidatos.slice()).filter(c => !nombresUsados.has(c[0].toLowerCase())).slice(0, faltan);
    if (elegidos.length < faltan) throw new Error("No hay suficientes nombres de productos nuevos (faltan " + (faltan - elegidos.length) + "). Agrega mas a 'candidatos'.");
    var nuevos = elegidos.map((c, i) => ({
        codigo: "PRD-" + pad(maxNum + i + 1, 3),
        nombre: c[0],
        categoria: c[1],
        precioVenta: rint(25, 140) * 100,
        cantidad: NumberInt(0),
        unidadMedida: (function () { var r = Math.random(); return r < 0.5 ? "kg" : (r < 0.8 ? "libra" : "unidad"); })(),
        estado: ESTADO_PRODUCTO,
        esDePrueba: true,
        _class: PKG + "Producto"
    }));
    db.productos.insertMany(nuevos);
    print("  Productos nuevos creados: " + nuevos.length);
}
var productos = db.productos.find().toArray();
print("  Productos en total: " + productos.length);

// ===================== 4) ESTADO DE CADA PRODUCTO Y SUS PROVEEDORES =====================
var provReales = {};
db.compras.find().forEach(c => {
    var prov = c.proveedorId || (c.proveedor_id ? hex(c.proveedor_id) : null);
    (c.detalle || []).forEach(d => {
        var pid = d.productoId || (d.producto_id ? hex(d.producto_id) : null);
        if (!prov || !pid) return;
        if (!provReales[pid]) provReales[pid] = [];
        if (provReales[pid].indexOf(prov) < 0) provReales[pid].push(prov);
    });
});

var realCodes = new Set(["PRD-001", "PRD-002", "PRD-003", "PRD-004", "PRD-005"]);
var estadoProd = {};   // DISP, AGOT, SIN
var pool = [];
productos.forEach(p => {
    var id = hex(p._id);
    if (provReales[id]) estadoProd[id] = "DISP";
    else if (realCodes.has(p.codigo)) estadoProd[id] = "SIN";
    else pool.push(id);
});
mezclar(pool);
var sinYa = Object.keys(estadoProd).filter(k => estadoProd[k] === "SIN").length;
var nSin = Math.max(0, Math.round(productos.length * PCT_SIN_STOCK) - sinYa);
var nAgot = Math.round(productos.length * PCT_AGOTADO);
pool.forEach((id, i) => { estadoProd[id] = i < nSin ? "SIN" : (i < nSin + nAgot ? "AGOT" : "DISP"); });

// Aviso: productos "sin stock" que en realidad tienen cantidad > 0
productos.forEach(p => {
    if (estadoProd[hex(p._id)] === "SIN" && (p.cantidad || 0) !== 0) {
        print("  AVISO: " + p.codigo + " (" + p.nombre + ") queda 'sin stock' pero tiene cantidad=" + p.cantidad);
    }
});

var provDeProd = {};
var rot = 0;
productos.forEach(p => {
    var id = hex(p._id);
    if (estadoProd[id] === "SIN") return;
    var ids = (provReales[id] || []).filter(x => activosSet.has(x)).slice(0, 3);
    if (ids.length === 0) {
        var r = Math.random();
        var n = r < 0.55 ? 1 : (r < 0.90 ? 2 : 3);
        ids = [hex(activos[rot % activos.length]._id)];
        rot++;
        var intentos = 0;
        while (ids.length < n && intentos < 100) {
            var cand = hex(elegir(activos)._id);
            if (ids.indexOf(cand) < 0) ids.push(cand);
            intentos++;
        }
    }
    provDeProd[id] = ids.map(pid => ({
        id: pid,
        precio: Math.max(50, Math.round(p.precioVenta * rnd(0.50, 0.65) / 50) * 50)
    }));
});
function elegirProv(lista) {
    if (lista.length === 1) return lista[0];
    var r = Math.random();
    if (lista.length === 2) return r < 0.6 ? lista[0] : lista[1];
    return r < 0.5 ? lista[0] : (r < 0.8 ? lista[1] : lista[2]);
}
// Costo promedio del producto entre sus proveedores (para desperdicios)
function costoPromedio(id) {
    var l = provDeProd[id];
    var s = 0; l.forEach(x => { s += x.precio; });
    return Math.max(50, Math.round(s / l.length / 50) * 50);
}

// ===================== 5) CANTIDADES A GENERAR =====================
var nVentas = META.ventas - db.ventas.countDocuments();
var nDesp = META.desperdicio - db.registrosDesperdicio.countDocuments();
var nCompras = META.compras - db.compras.countDocuments();
var movReales = db.movimientosPuntos.countDocuments();
var nRed = META.movimientos - movReales - nVentas;
if (nVentas < 0 || nDesp < 0 || nCompras < 0 || nRed < 0) throw new Error("Las metas son menores que lo que ya existe.");
var otroTipo = db.movimientosPuntos.findOne({ tipo: { $ne: TIPO_ACUMULACION } });
var TIPO_RED = otroTipo ? otroTipo.tipo : TIPO_REDENCION_DEFECTO;
if (!otroTipo) print("  AVISO: no hay movimientos de otro tipo en la base; se usara '" + TIPO_RED + "'. Verifica que exista en tu enum.");
print("--- 5) A generar: ventas=" + nVentas + ", compras=" + nCompras + ", desperdicio=" + nDesp + ", redenciones=" + nRed + " ---");

// ===================== 6) VENTAS Y DESPERDICIO (movimientos de salida) =====================
var stocked = productos.filter(p => estadoProd[hex(p._id)] !== "SIN");
var baseVentas = Math.min(3, Math.floor(nVentas / stocked.length));
if (baseVentas < 1) throw new Error("Hay muy pocas ventas (" + nVentas + ") para cubrir " + stocked.length + " productos con stock.");
var cum = [];
var acc = 0;
stocked.forEach(p => { acc += rnd(0.4, 2.0); cum.push(acc); });
function elegirProducto() {
    var x = Math.random() * acc, lo = 0, hi = cum.length - 1;
    while (lo < hi) { var mid = (lo + hi) >> 1; if (cum[mid] >= x) hi = mid; else lo = mid + 1; }
    return stocked[lo];
}
var eventos = {};
stocked.forEach(p => { eventos[hex(p._id)] = []; });
// Peso de cada dia: hay dias fuertes y dias flojos (viernes a domingo un poco mas altos)
var cumDia = [];
var accDia = 0;
for (let d = 0; d < diasTotales; d++) {
    var dow = new Date(inicio.getTime() + d * DIA).getUTCDay();   // 0 = domingo
    var pw = (dow === 0 || dow === 5 || dow === 6) ? 1.4 : 1.0;
    var pr = Math.random();
    pw *= pr < 0.15 ? rnd(2.0, 2.8) : (pr < 0.35 ? rnd(0.25, 0.5) : rnd(0.7, 1.3));
    accDia += pw;
    cumDia.push(accDia);
}
function diaAleatorio(maxDia) {
    var x = Math.random() * cumDia[maxDia - 1], lo = 0, hi = maxDia - 1;
    while (lo < hi) { var mid = (lo + hi) >> 1; if (cumDia[mid] >= x) hi = mid; else lo = mid + 1; }
    return lo;
}
// Cantidad por venta: mayoria bajas, algunas medianas y pocas altas
function cantVenta() {
    var r = Math.random();
    if (r < 0.65) return rint(1, MAX_CANT_VENTA);   // bajas
    if (r < 0.90) return rint(4, 8);                // medias
    return rint(9, 15);                             // altas
}
function fechaEvento(id) {
    var maxDia = estadoProd[id] === "AGOT" ? diasAgotado : diasTotales;
    var dia = diaAleatorio(maxDia);
    return new Date(inicio.getTime() + dia * DIA + (13 * 60 + Math.floor(Math.random() * 600)) * 60000 + Math.floor(Math.random() * 60000));
}

var asignacion = [];
stocked.forEach(p => { for (let k2 = 0; k2 < baseVentas; k2++) asignacion.push(p); });
while (asignacion.length < nVentas) asignacion.push(elegirProducto());
mezclar(asignacion);
var ventas = asignacion.map(p => {
    var id = hex(p._id);
    var v = { _id: new ObjectId(), p: p, id: id, cant: cantVenta(), fecha: fechaEvento(id) };
    eventos[id].push({ t: v.fecha.getTime(), cant: v.cant });
    return v;
});

// Motivos iguales a los del formulario de desperdicios, con su peso (suman 100)
var motivos = [["Producto vencido", 25], ["Fruta golpeada", 30], ["Producto dañado", 15], ["Mala conservación", 25], ["Otro", 5]];
function elegirMotivo() {
    var r = Math.random() * 100, acum = 0;
    for (let i = 0; i < motivos.length; i++) { acum += motivos[i][1]; if (r < acum) return motivos[i][0]; }
    return motivos[0][0];
}
var desperdicios = [];
for (let i = 0; i < nDesp; i++) {
    var pd = elegirProducto();
    var idd = hex(pd._id);
    var cd = rint(1, 3);
    var fd = fechaEvento(idd);
    eventos[idd].push({ t: fd.getTime(), cant: cd });
    desperdicios.push({ p: pd, id: idd, cant: cd, fecha: fd });
}

// ===================== 7) COMPRAS (ingresos de mercancia) =====================
var ids = stocked.map(p => hex(p._id));
var m = ids.map(id => eventos[id].length);
var M = m.reduce((a, b) => a + b, 0);
if (M < nCompras) throw new Error("Hay muy pocas salidas (" + M + ") para justificar " + nCompras + " compras.");
var k = m.map(x => Math.min(x, Math.max(1, Math.round(nCompras * x / M))));
var suma = k.reduce((a, b) => a + b, 0);
var guard = 0;
while (suma !== nCompras && guard < 500000) {
    var j = Math.floor(Math.random() * ids.length);
    if (suma < nCompras && k[j] < m[j]) { k[j]++; suma++; }
    else if (suma > nCompras && k[j] > 1) { k[j]--; suma--; }
    guard++;
}
if (suma !== nCompras) throw new Error("No se pudo ajustar el numero de compras: " + suma + " de " + nCompras);

var compras = [];
var ajustes = [];
stocked.forEach((p, ix) => {
    var id = ids[ix];
    var ev = eventos[id].sort((a, b) => a.t - b.t);
    var stockFinal = estadoProd[id] === "AGOT" ? 0 : (Math.random() < 0.10 ? rint(1, 5) : rint(15, 60));

    var idxs = [];
    for (let q = 1; q < ev.length; q++) idxs.push(q);
    mezclar(idxs);
    var disp = [0].concat(idxs.slice(0, k[ix] - 1)).sort((a, b) => a - b);

    for (let d = 0; d < disp.length; d++) {
        var desde = disp[d];
        var hasta = (d + 1 < disp.length) ? disp[d + 1] : ev.length;
        var cantidad = (d === 0 ? stockFinal : 0);
        for (let q = desde; q < hasta; q++) cantidad += ev[q].cant;
        var prov = elegirProv(provDeProd[id]);
        var t = Math.max(inicio.getTime() + 3600000, ev[desde].t - Math.floor(rnd(30, 180)) * 60000);
        compras.push({
            fecha: new Date(t),
            usuarioId: hex(elegir(compradores)._id),
            proveedorId: prov.id,
            detalle: [{ productoId: id, nombre: p.nombre, precioCompra: prov.precio, cantidad: NumberInt(cantidad), subtotal: cantidad * prov.precio }],
            total: cantidad * prov.precio,
            esDePrueba: true,
            _class: PKG + "Compra"
        });
    }

    var upd = { $inc: { cantidad: NumberInt(stockFinal) }, $set: { ajusteStockPrueba: NumberInt(stockFinal) } };
    if (p.precioCompra === undefined || p.precioCompra === null) {
        upd.$set.precioCompra = provDeProd[id][0].precio;
        upd.$set.precioCompraDePrueba = true;
    }
    ajustes.push({ updateOne: { filter: { _id: p._id }, update: upd } });
});
compras.sort((a, b) => a.fecha - b.fecha);
compras.forEach((c, i) => { c.codigo = "COM-T" + pad(i + 1, 4); });

// ===================== 8) VENTAS CON PUNTOS =====================
var saldo = {};
var saldoIni = {};
clientes.forEach(c => { saldo[hex(c._id)] = c.puntos || 0; saldoIni[hex(c._id)] = c.puntos || 0; });
ventas.sort((a, b) => a.fecha - b.fecha);
var pend = nRed;
var restantes = ventas.length;
var docsVentas = [];
var movs = [];
ventas.forEach((v, i) => {
    var cli = elegir(clientes);
    var cid = hex(cli._id);
    var usr = elegir(cajeros);
    var precio = v.p.precioVenta;
    var subtotal = v.cant * precio;
    // Si el cliente tambien es usuario activo del sistema, la app le da 25% de descuento
    var accCli = cli.accesoSistema;
    var esUsuarioSis = !!accCli && accCli.habilitado !== false && String(accCli.estado).toUpperCase() === "ACTIVO";
    var descuentoUsuario = esUsuarioSis ? Math.round(subtotal * PORCENTAJE_DESCUENTO_USUARIO * 100) / 100 : 0;
    var base = subtotal - descuentoUsuario;
    var usar = 0;
    if (pend > 0 && saldo[cid] >= 10000 && Math.random() < Math.min(1, 1.25 * pend / restantes)) {
        // descuento entre 10% y 30% de la compra (el sistema permite maximo 30%)
        usar = Math.floor(base * rnd(0.10, 0.30) / 100) * 100;
        usar = Math.floor(Math.min(usar, saldo[cid], base));
        if (usar < 100) usar = 0;
    }
    restantes--;
    var totalFinal = base - usar;
    var ganados = Math.floor(totalFinal * 0.10);
    saldo[cid] = saldo[cid] - usar + ganados;
    docsVentas.push({
        _id: v._id,
        codigo: "VEN-T" + pad(i + 1, 6),
        factura: "FAC-T" + pad(i + 1, 6),
        fecha: v.fecha,
        usuarioId: hex(usr._id),
        clienteId: cid,
        detalle: [{ productoId: v.id, nombre: v.p.nombre, precioVenta: precio, cantidad: NumberInt(v.cant), subtotal: subtotal }],
        total: totalFinal,
        puntosGanados: NumberInt(ganados),
        puntosUtilizados: NumberInt(usar),
        descuentoEmpleado: descuentoUsuario,
        descuento: usar,
        esDePrueba: true,
        _class: PKG + "Venta"
    });
    movs.push({ clienteId: cid, ventaId: hex(v._id), tipo: TIPO_ACUMULACION, puntos: NumberInt(ganados),
                valorDescuento: 0, fecha: v.fecha, esDePrueba: true, _class: PKG + "MovimientoPuntos" });
    if (usar > 0) {
        pend--;
        movs.push({ clienteId: cid, ventaId: hex(v._id), tipo: TIPO_RED, puntos: NumberInt(usar),
                    valorDescuento: usar, fecha: new Date(v.fecha.getTime() + 1000), esDePrueba: true,
                    _class: PKG + "MovimientoPuntos" });
    }
});
if (pend > 0) print("  AVISO: faltaron " + pend + " redenciones por saldos insuficientes");

// ===================== 9) DESPERDICIO =====================
desperdicios.sort((a, b) => a.fecha - b.fecha);
var docsDesp = desperdicios.map((d, i) => {
    var costo = costoPromedio(d.id);
    return {
        codigo: "DES-T" + pad(i + 1, 4),
        fecha: d.fecha,
        usuarioId: hex(elegir(admins)._id),
        motivo: elegirMotivo(),
        detalle: [{ productoId: d.id, nombre: d.p.nombre, cantidad: NumberInt(d.cant), costoUnitario: costo, subtotal: d.cant * costo }],
        total: d.cant * costo,
        esDePrueba: true,
        _class: PKG + "RegistroDesperdicio"
    };
});

// ===================== 10) GUARDAR TODO =====================
print("--- 10) Guardando ---");
insertar("compras", compras);
insertar("ventas", docsVentas);
insertar("movimientosPuntos", movs);
insertar("registrosDesperdicio", docsDesp);
if (ajustes.length) db.productos.bulkWrite(ajustes);

var opsPersonas = [];
clientes.forEach(c => {
    var id = hex(c._id);
    var neto = saldo[id] - saldoIni[id];
    if (neto !== 0) {
        opsPersonas.push({ updateOne: { filter: { _id: c._id },
            update: { $inc: { puntos: NumberInt(neto) }, $set: { ajustePuntosPrueba: NumberInt(neto) } } } });
    }
});
if (opsPersonas.length) db.personas.bulkWrite(opsPersonas);

// ===================== VALIDACIONES FINALES =====================
print("--- Validaciones ---");
var negativos = db.productos.countDocuments({ cantidad: { $lt: 0 } });
print("  Productos con stock negativo: " + negativos);
var puntosNeg = db.personas.countDocuments({ funciones: "CLIENTE", puntos: { $lt: 0 } });
print("  Clientes con puntos negativos: " + puntosNeg);

// ===================== RESUMEN =====================
print("=== RESUMEN ===");
var tot = 0;
["personas", "productos", "compras", "ventas", "registrosDesperdicio", "movimientosPuntos"].forEach(c => {
    var n = db[c].countDocuments();
    tot += n;
    print("  " + c + ": " + n);
});
print("  TOTAL: " + tot);
var conteo = { DISP: 0, AGOT: 0, SIN: 0 };
Object.keys(estadoProd).forEach(id => { conteo[estadoProd[id]]++; });
print("  Disponible: " + conteo.DISP + " | Agotado: " + conteo.AGOT + " | Sin stock: " + conteo.SIN);
print("  Desperdicios por motivo:");
db.registrosDesperdicio.aggregate([{ $group: { _id: "$motivo", n: { $sum: 1 } } }, { $sort: { n: -1 } }])
    .forEach(d => print("    " + d._id + ": " + d.n));