# AsyncPayments

AsyncPayments é um aplicativo Android para realização de pagamentos assíncronos entre usuários. O app suporta múltiplos métodos de comunicação (Internet, NFC, Bluetooth, SMS) e utiliza autenticação JWT para segurança.

---

## Funcionalidades

- **Cadastro e Login de Usuário**
  - Cadastro com e-mail, senha, CPF, nome, sobrenome e celular.
  - Consentimento LGPD para uso de dados.
  - Login com autenticação JWT.
  - Armazenamento seguro do token para requisições autenticadas.

- **Transações**
  - Envio de pagamentos entre usuários.
  - Seleção do método de comunicação: Internet, NFC, Bluetooth, SMS.
  - Adição de fundos à conta assíncrona.
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
- **AddFundsActivity**: Tela para adicionar fundos à conta assíncrona.


## Backend


O app espera um backend REST rodando em `http://10.0.2.2:8080/` (localhost para emulador Android).

### **Endpoints Principais**

#### **Autenticação**
- `POST /auth/register` - Registrar um novo usuário.
- `POST /auth/login` - Login e obtenção do token JWT.
- `GET /auth/me/id` - Obter o ID do usuário logado.
- `GET /auth/user/id?email={email}` - Buscar ID de um usuário pelo e-mail.
- `GET /auth/test` - Testar autenticação.

#### **Usuários**
- `GET /usuarios` - Listar todos os usuários (ADMIN).
- `GET /usuarios/{id}` - Buscar usuário por ID (ADMIN).
- `PUT /usuarios/{id}` - Atualizar informações de um usuário (ADMIN).
- `DELETE /usuarios/{id}` - Deletar um usuário (ADMIN).
- `GET /usuarios/me` - Obter informações do usuário logado.
- `POST /usuarios/me/aceitar-kyc` - Aceitar validação de identidade (KYC).
- `POST /usuarios/me/anonimizar` - Anonimizar dados do usuário.

#### **Transações**
- `GET /transacoes` - Listar todas as transações (ADMIN).
- `GET /transacoes/{id}` - Buscar transação por ID (ADMIN).
- `POST /transacoes` - Criar uma nova transação.
- `POST /transacoes/adicionar-fundos` - Adicionar fundos à conta assíncrona.

#### **Sincronização**
- `POST /sincronizacao/manual` - Sincronizar todas as contas.
- `POST /sincronizacao/manual/{id}` - Sincronizar uma conta específica.

---

## Novos Endpoints

### **Buscar Transação por ID**
- **URL:** `GET /transacoes/{id}`
- **Descrição:** Busca uma transação pelo ID e retorna os dados da transação junto com o status atual.
- **Retorno:**
  - **200 OK:** Retorna um JSON contendo a transação e o status.
    ```json
    {
      "transacao": {
        "id": 123,
        "idUsuarioOrigem": 1,
        "idUsuarioDestino": 2,
        "valor": 500.0,
        "descricao": "Transferência entre contas",
        "sincronizada": true
      },
      "status": "SINCRONIZADA"
    }
    ```
  - **404 Not Found:** Retorna uma mensagem indicando que a transação não foi encontrada.
    ```json
    {
      "message": "Transação não encontrada."
    }
    ```

### **Consultar Status da Transação**
- **URL:** `GET /transacoes/{id}/status`
- **Descrição:** Consulta o status atual de uma transação pelo ID.
- **Retorno:**
  - **200 OK:** Retorna o status da transação em formato de texto.
    ```
    Status da transação 123: SINCRONIZADA
    ```
  - **404 Not Found:** Retorna uma mensagem indicando que a transação não foi encontrada.
    ```
    Transação não encontrada.
    ```

---

## Alterações no Sistema

### **Rollback de Transações**
- Implementado fluxo de rollback para transações que excedem o prazo de 72 horas sem sincronização.
- Atualiza o campo `descricao` da transação com a mensagem: `"Rollback: Transação não sincronizada em 72h."`.
- Marca o status da transação como `ROLLBACK` e bloqueia as contas envolvidas.

### **Enum StatusTransacao**
- Criado para gerenciar os estados das transações:
  - `PENDENTE`: Transação aguardando processamento.
  - `SINCRONIZADA`: Transação sincronizada com sucesso.
  - `ROLLBACK`: Transação revertida devido a falha ou atraso.
  - `ERRO`: Transação com erro durante o processamento.

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
