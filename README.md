# Atlas

Aplicativo Android nativo para organização familiar, desenvolvido com Kotlin,
Jetpack Compose e Firebase.

## Requisitos

- JDK 17
- Android SDK com a plataforma Android 35
- Android Studio (ou Gradle 8.14.4 para linha de comando)
- Android 10 (API 29) ou superior para executar o aplicativo

## Executar

Abra o projeto no Android Studio e aguarde a sincronização do Gradle. Para gerar
o APK de debug pela linha de comando:

```bash
./gradlew assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Estrutura

- `app/src/main/java/com/example/atlas/ui/screens`: telas do aplicativo.
- `app/src/main/java/com/example/atlas/ui/navigation`: rotas e navegação Compose.
- `app/src/main/java/com/example/atlas/data/model`: modelos de dados.
- `app/src/main/java/com/example/atlas/data/repository`: acesso aos serviços de dados.
- `app/src/main/java/com/example/atlas/ui/screens/auth`: interface de autenticação.
- `app/src/main/AndroidManifest.xml`: configuração e atividade inicial do app.

O painel inicial exibe os atalhos das funcionalidades. Compras, calendário,
orçamento, mensagens e mapa navegam para as telas já incluídas. O cartão
“Documentos” permite armazenar links e informações da família. O cartão
“Horário” e os atalhos de aniversários, refeições, receitas, família e galeria
abrem telas placeholder. A tela Tarefas continua disponível na barra inferior.

## Agenda

A tela Agenda permite selecionar um dia entre os dias próximos, visualizar os
eventos dessa data e adicionar eventos com título, descrição, horário e local.
Os eventos são persistidos localmente em SQLite por Room 2.8.4 e continuam
disponíveis após fechar e reabrir o app. A coleção `calendar_events` do
Firestore sincroniza alterações quando a tela abre ou um evento é adicionado;
se não houver conexão, os dados locais são preservados para nova tentativa.

## Lista de compras

A tela Compras permite adicionar itens com nome, categoria e quantidade,
marcá-los como comprados e removê-los. Os itens ficam persistidos localmente
com Room e mostram quem os adicionou; a contagem de itens pendentes também é
exibida no painel inicial. O repositório sincroniza a coleção `shopping_items`
com Cloud Firestore ao abrir a tela e após alterações. Se uma operação na nuvem
falhar, a alteração local permanece e é tentada novamente em uma sincronização
posterior; exclusões offline são guardadas como pendências locais. Requer um
banco Firestore criado e regras de segurança configuradas no Firebase Console.

## Documentos

A tela Documentos armazena nome, descrição e link HTTP/HTTPS, permite abrir o
link em outro aplicativo e remover o registro. Os metadados são persistidos no
Room e sincronizados com a coleção Firestore `family_documents`, incluindo
exclusões offline pendentes. Não há upload nem armazenamento dos arquivos em si.

## Firebase e login Google

O projeto inclui Firebase Authentication e Firebase Analytics pelo Firebase
BoM 35.1.0, Google Services Gradle Plugin 4.5.0 e Credential Manager para
seleção da conta Google. O login troca o token Google por uma credencial
Firebase; a sessão controla o acesso à tela principal e pode ser encerrada pelo
botão de saída.

O `applicationId` é `com.android.application`, conforme o app registrado em
`app/google-services.json`; o namespace Kotlin continua `com.example.atlas`.
Não altere esse identificador sem registrar o novo pacote no Firebase e baixar
um JSON correspondente.

Para habilitar o login no Firebase Console:

1. Ative **Authentication → Sign-in method → Google**.
2. Cadastre as impressões digitais SHA-1 e SHA-256 das chaves de assinatura
   usadas para debug e/ou release.
3. Baixe novamente `google-services.json` após alterações no cadastro OAuth e
   coloque-o em `app/google-services.json`.
4. Gere e instale o app com `./gradlew assembleDebug`.

O Firebase Analytics está incluído; ainda não foram adicionados eventos
personalizados do Atlas.

## Tecnologias e versões principais

- Android Gradle Plugin 8.12.1.
- Kotlin e plugin Compose 2.4.20.
- Gradle Wrapper 8.14.4.
- Firebase BoM 35.1.0: Authentication, Cloud Firestore e Analytics.
- Room 2.8.4 com processador KSP 2.3.12.

Room usa entidades e DAOs com observação reativa no banco local `atlas_database`
(versão 5, com migrações da versão anterior). Eventos, itens de compras,
registros de documentos e pendências de exclusão são armazenados no dispositivo.

O Firestore usa as coleções `shopping_items`, `calendar_events` e
`family_documents`. Configure regras para restringir leitura e escrita a
usuários autorizados antes de distribuir o app; as coleções ainda não têm
separação por família, pois o modelo de associação familiar não foi implementado.