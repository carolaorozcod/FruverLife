package com.FruverLifes.Service;

import com.FruverLifes.Model.Persona;
import com.FruverLifes.Repositories.PersonasRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private static final String CLIENTE = "CLIENTE";

    @Autowired
    private PersonasRepositories personasRepository;

    public List<Persona> listarClientes() {
        return personasRepository.findByFuncionesContaining(CLIENTE);
    }

    public Persona buscarPorId(String id) {
        return personasRepository.findByIdAndFuncionesContaining(id, CLIENTE).orElse(null);
    }

    public Persona buscarPorIdentificacion(String identificacion) {
        return personasRepository.findByIdentificacion(identificacion)
                .filter(p -> p.getFunciones() != null && p.getFunciones().contains(CLIENTE))
                .orElse(null);
    }

    public Persona guardar(Persona cliente) {
        if (cliente.getFunciones() == null) {
            cliente.setFunciones(new ArrayList<>());
        }
        if (!cliente.getFunciones().contains(CLIENTE)) {
            cliente.getFunciones().add(CLIENTE);
        }
        return personasRepository.save(cliente);
    }

    public void eliminar(String id) {
        Optional<Persona> opt = personasRepository.findById(id);
        if (opt.isEmpty()) return;

        Persona persona = opt.get();
        persona.getFunciones().remove(CLIENTE);

        if (persona.getFunciones().isEmpty()) {
            personasRepository.deleteById(id);   // solo era cliente
        } else {
            personasRepository.save(persona);    // también es proveedor/usuario: solo se quita el rol
        }
    }
}