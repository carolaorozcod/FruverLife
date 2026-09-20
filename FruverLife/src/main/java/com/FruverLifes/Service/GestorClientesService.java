package com.FruverLifes.Service;

import com.FruverLifes.Model.Clientes;
import com.FruverLifes.Repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GestorClientesService {

    @Autowired
    private ClienteRepository clientesRepository;
    public void registrarCliente(Clientes cliente) {
        clientesRepository.save(cliente);
    }
    public List<Clientes> listars() {
        return clientesRepository.findAll();
    }
    public void eliminar(int idCliente) {
        clientesRepository.deleteById(idCliente);
    }
}