# AsyncPayments

AsyncPayments é um aplicativo Android para realização de pagamentos assíncronos entre usuários. O app suporta múltiplos métodos de comunicação (Internet, NFC, Bluetooth, SMS) e utiliza autenticação JWT para segurança.

---

## Funcionalidades

- **Cadastro e Login de Usuário**
  - Cadastro com e-mail e senha.
  - Login com autenticação JWT.
  - Armazenamento seguro do token para requisições autenticadas.

- **Transações**
  - Envio de pagamentos entre usuários.
  - Seleção do método de comunicação: Internet, NFC, Bluetooth, SMS.
  - Visualização do histórico de transações.
  - Sincronização manual de transações offline.

- **Segurança**
  - Comunicação com backend via HTTPS.
  - Uso de token JWT em todas as operações sensíveis.
  - Criptografia AES-256 para dados sensíveis e transmissões offline.

---

## Estrutura do Projeto

```
app/
 ├── src/
 │    ├── main/
 │    │    ├── java/com/example/asyncpayments/
 │    │    │    ├── comms/           # Comunicação (NFC, Bluetooth, SMS, Internet)
 │    │    │    ├── model/           # Modelos de dados (Request/Response)
 │    │    │    ├── network/         # Retrofit, Interceptors, Services
 │    │    │    ├── ui/              # Activities e Fragments
 │    │    │    ├── domain/          # Lógica de negócio
 │    │    │    └── utils/           # Utilitários (SharedPreferences, Constantes)
 │    │    ├── res/
 │    │    │    ├── layout/          # Layouts XML das telas
 │    │    │    ├── drawable/        # Imagens, vetores, fundos
 │    │    │    ├── values/          # Cores, strings, temas, estilos
 │    │    │    └── xml/             # Configurações de segurança
 │    │    └── AndroidManifest.xml
 │    └── ...
 └── ...
```

---

## Principais Telas

- **LoginActivity**: Tela de login do usuário.
- **RegisterActivity**: Tela de cadastro.
- **TransactionActivity**: Tela principal para realizar e visualizar transações.
- **HomeActivity**: Tela inicial com informações de saldo e sincronização.
- **ProfileActivity**: Tela de perfil do usuário.

---

## Backend

- O app espera um backend REST rodando em `http://10.0.2.2:8080/` (localhost para emulador Android).
- Endpoints principais:
  - `/auth/login` - Login e obtenção do token JWT.
  - `/register` - Cadastro de usuário.
  - `/transacoes/realizar` - Realizar transação.
  - `/transacoes/todas` - Listar transações.
  - `/usuarios/me` - Obter informações do usuário logado.

---

## Como rodar

1. **Clone o repositório**
   ```sh
   git clone https://github.com/seu-usuario/AsyncPayments.git
   cd AsyncPayments
   ```

2. **Abra no Android Studio ou VS Code**

3. **Configure o backend**
   - Certifique-se de que o backend está rodando em `http://10.0.2.2:8080/`.

4. **Compile e execute o app**
   - Use um emulador Android ou dispositivo físico.

---

## Fluxo de Sincronização

- **Transações assíncronas**: Ficam armazenadas localmente até que o usuário clique em "Sincronizar contas".
- **Sincronização manual**: O usuário pode sincronizar as contas a qualquer momento pelo botão na tela inicial.
- **Transações offline**: São enfileiradas e enviadas ao backend assim que houver conexão e o usuário solicitar sincronização.

---

## Observações

- A sincronização de contas é manual, via botão na tela inicial.
- O backend deve estar rodando e acessível pelo endereço configurado.
- O app não sincroniza automaticamente ao reconectar à internet, garantindo controle total ao usuário.
- Para desenvolvimento local, utilize o endereço `http://10.0.2.2:8080/` no emulador Android.

---

## Contribuição

Contribuições são bem-vindas!  
Para contribuir:

1. Fork este repositório
2. Crie uma branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas alterações (`git commit -am 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

---

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](../LICENSE) para mais detalhes.
