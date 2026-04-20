package com.pucpr.handlers;

import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;

import java.io.InputStream;
import java.util.Map;

public class AuthHandler {
    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public void handleLogin(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            InputStream is = exchange.getRequestBody();
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> dados = mapper.readValue(is, Map.class);

            String email = dados.get("email");
            String senha = dados.get("password");

            Optional<Usuario> usuarioOpt = repository.findByEmail(email);

            if (usuarioOpt.isEmpty()) {
                String response = "{\"message\":\"E-mail ou senha inválidos\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(401, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            Usuario usuario = usuarioOpt.get();

            if (!BCrypt.checkpw(senha, usuario.getSenhaHash())) {
                String response = "{\"message\":\"E-mail ou senha inválidos\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(401, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            String token = jwtService.generateToken(usuario);

            String response = "{\"token\": \"" + token + "\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();

        } catch (Exception e) {
            e.printStackTrace();
            String response = "{\"message\":\"Erro no login\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        }
    }

    public void handleRegister(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            InputStream is = exchange.getRequestBody();
            Map<String, String> dados = mapper.readValue(is, Map.class);

            String email = dados.get("email");
            String senha = dados.get("password");

            if (repository.findByEmail(email).isPresent()) {
                String response = "E-mail já cadastrado";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                return; // 🔥 MUITO IMPORTANTE
            }

            String senhaHash = BCrypt.hashpw(senha, BCrypt.gensalt(12));

            Usuario usuario = new Usuario(
                    "Usuario",
                    email,
                    senhaHash,
                    "PACIENTE"
            );

            repository.save(usuario);

            String response = "{\"message\":\"Usuário cadastrado com sucesso\"}";

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
            return; // garante que para aqui

        } catch (Exception e) {
            e.printStackTrace();

            String response = "Erro ao cadastrar";

            try {
                exchange.sendResponseHeaders(500, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            } catch (Exception ignored) {
            }
        }
    }
}