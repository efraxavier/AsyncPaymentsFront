# AsyncPayments

AsyncPayments é um aplicativo Android para realização de pagamentos assíncronos entre usuários. O app suporta múltiplos métodos de comunicação (Internet, NFC, Bluetooth, SMS) e utiliza autenticação JWT para segurança.

## Funcionalidades

- **Cadastro e Login de Usuário**
  - Cadastro com e-mail e senha.
  - Login com autenticação JWT.
  - Armazenamento seguro do token para requisições autenticadas.

- **Transações**
  - Envio de pagamentos entre usuários.
  - Seleção do método de comunicação: Internet, NFC, Bluetooth, SMS.
  - Visualização do histórico de transações.

- **Segurança**
  - Comunicação com backend via HTTPS.
  - Uso de token JWT em todas as operações sensíveis.

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
 │    │    │    └── utils/           # Utilitários (SharedPreferences, Constantes)
 │    │    ├── res/
 │    │    │    ├── layout/          # Layouts XML das telas
 │    │    │    ├── drawable/        # Imagens, vetores, fundos (hexágonos)
 │    │    │    ├── values/          # Cores, strings, temas, estilos
 │    │    │    └── xml/             # Configurações de segurança
 │    │    └── AndroidManifest.xml
 │    └── ...
 └── ...
```

## Principais Telas

- **LoginActivity**: Tela de login do usuário.
- **RegisterActivity**: Tela de cadastro.
- **TransactionActivity**: Tela principal para realizar e visualizar transações.
- **MainActivity**: Tela inicial e navegação.

## Backend

- O app espera um backend REST rodando em `http://10.0.2.2:8080/` (localhost para emulador Android).
- Endpoints principais:
  - `/auth/login` - Login e obtenção do token JWT.
  - `/register` - Cadastro de usuário.
  - `/transacoes/realizar` - Realizar transação.
  - `/transacoes/todas` - Listar transações.

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
