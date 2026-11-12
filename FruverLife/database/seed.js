db = db.getSiblingDB("inventario_db");

db.usuarios.insertOne({
  _id: ObjectId("6aeb641ee4b1386ce6e96dab"),
  nombre: "Kevin Osorio",
  identificacion: "1000000001",
  telefono: "3001234567",
  correo: "osorioorozcokevinandress@gmail.com",
  funciones: ["EMPLEADO"],
  accesoSistema: {
    habilitado: true,
    usuario: "kevin.admin",
    contrasenaHash: "hash_simulado_123",
    roles: ["GERENTE"],
    estado: "ACTIVO"
  }
});