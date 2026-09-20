package com.FruverLifes.Service;
import com.FruverLifes.Model.Clientes;
import com.FruverLifes.Repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public List<Clientes> listarClientes() {
        return clienteRepository.findAll();
    }

    public Clientes buscarPorId(int id) {
        return clienteRepository.findById(id).orElse(null);
    }

    public Clientes guardar(Clientes cliente) {
        return clienteRepository.save(cliente);
    }

    public void eliminar(int id) { clienteRepository.deleteById(id);}
}