# AsyncPayments

Aplicativo Android para transações financeiras assíncronas e síncronas.

## Funcionalidades

- Cadastro e login de usuários
- Visualização de saldo em contas síncronas e assíncronas
- Envio e recebimento de transações (online e offline)
- Sincronização manual de contas (via botão)
- Notificações de transações recebidas
- Suporte a transações offline (fila local)
- Histórico de transações
- Máscara de saldo (mostrar/ocultar)
- Suporte a múltiplos métodos de conexão

## Tecnologias Utilizadas

- **Kotlin** (linguagem principal)
- **Android SDK** (ViewBinding, Lifecycle)
- **Retrofit** (requisições HTTP)
- **OkHttp** (logging de rede)
- **Coroutines** (operações assíncronas)
- **Room** (opcional, se houver persistência local)
- **SharedPreferences** (armazenamento simples)
- **Material Design** (UI)

## Como rodar

1. Clone o repositório:
    ```sh
    git clone https://github.com/seu-usuario/AsyncPayments.git
    ```
2. Abra o projeto no Android Studio.
3. Configure o backend na constante `BASE_URL` em [`Constants.kt`](src/main/java/com/example/asyncpayments/utils/Constants.kt).
4. Certifique-se de que o backend está rodando e acessível pelo endereço configurado.
5. Execute o app em um emulador ou dispositivo físico.

## Estrutura do Projeto

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/asyncpayments/
│   │   │   ├── ui/         # Telas e atividades
│   │   │   ├── domain/     # Lógica de negócio
│   │   │   ├── network/    # Serviços de rede (Retrofit)
│   │   │   └── utils/      # Utilitários e helpers
│   │   └── res/            # Recursos (layouts, drawables, etc)
│   └── test/               # Testes unitários
├── build.gradle.kts
└── README.md
```

## Fluxo de Sincronização

- **Transações assíncronas**: Ficam armazenadas localmente até que o usuário clique em "Sincronizar contas".
- **Sincronização manual**: O usuário pode sincronizar as contas a qualquer momento pelo botão na tela inicial.
- **Transações offline**: São enfileiradas e enviadas ao backend assim que houver conexão e o usuário solicitar sincronização.

## Observações

- A sincronização de contas é manual, via botão na tela inicial.
- O backend deve estar rodando e acessível pelo endereço configurado.
- O app não sincroniza automaticamente ao reconectar à internet, garantindo controle total ao usuário.
- Para desenvolvimento local, utilize o endereço `http://10.0.2.2:8080/` no emulador Android.

## Contribuição

Contribuições são bem-vindas!  
Para contribuir:

1. Fork este repositório
2. Crie uma branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas alterações (`git commit -am 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](../LICENSE) para mais detalhes.