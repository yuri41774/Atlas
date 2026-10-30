# Relatório de implementação — Atlas

**Atualizado em:** 30/10/2026

## Visão geral

O Atlas é um projeto de aplicativo Android nativo para organização familiar,
implementado com Kotlin e Jetpack Compose. A estrutura de build e navegação está
configurada, e o painel inicial já apresenta atalhos para as áreas da família.
O app está configurado para Android 10 (API 29) ou superior.

## Configuração Android e build

- Projeto Gradle com módulo `:app`, Gradle Wrapper e configurações Kotlin DSL.
- Android Gradle Plugin 8.12.1, Kotlin 2.4.20, KSP 2.3.12 e Gradle Wrapper 8.14.4.
- Namespace Kotlin: `com.example.atlas`; identificador Android (`applicationId`):
  `com.android.application`, alinhado ao Firebase JSON recebido.
- `minSdk = 29` (Android 10), `compileSdk = 35` e `targetSdk = 35`.
- Compose habilitado, com compilação Kotlin direcionada ao Java 17.
- Manifesto configurado com o rótulo Atlas e `MainActivity` como atividade
  inicial/launcher.
- Regras de `.gitignore` para saídas de build, arquivos locais do Android Studio
  e `local.properties`.
- Dependências declaradas:
  - Activity Compose 1.10.0.
  - Navigation Compose 2.8.5.
  - Material 3 1.3.1.
  - Ícones estendidos Compose 1.7.6.
  - Firebase Authentication, Cloud Firestore e Analytics (Firebase BoM 35.1.0).
  - Credential Manager 1.3.0, Google ID 1.1.1 e coroutines Play Services 1.10.2.
  - Room 2.8.4 com processador de anotações KSP.

## Telas e navegação

- `MainActivity` inicia a interface Compose e exibe `MainScreen` dentro de
  `MaterialTheme` e `Surface`.
- `MainScreen` usa `Scaffold`, barra superior com o título “Atlas - Família”,
  barra de navegação inferior e `NavHost`.
- Rotas definidas: início, compras, agenda, tarefas, mural, finanças e locais.
- Também há rotas para horário, documentos, aniversários, refeições, receitas,
  família e galeria.
- A navegação inferior mostra início, compras, agenda, tarefas e mural; as
  demais telas podem ser acessadas pelos atalhos do painel.
- A navegação inferior usa restauração de estado e evita empilhar novamente a
  rota já selecionada.
- Telas disponíveis: início, compras, calendário, tarefas, mural, finanças,
  localização e documentos. Compras e Agenda têm armazenamento local e
  sincronização Firestore; Documentos gerencia metadados/links com persistência
  e sincronização. As outras telas de destino continuam sem fluxos completos.

## Painel inicial

- Implementado em Jetpack Compose como grade rolável com duas colunas e 12
  cartões, cada um com título, descrição, ícone, cor e ação.
- Cabeçalho “Nossa Família” e avatar inicial “Yu”.
- Atalhos ligados a rotas existentes:
  - Listas → compras.
  - Calendário → agenda.
  - Orçamento → finanças.
  - Documentos → cadastro sincronizado de metadados e links.
  - Aniversários, refeições, receitas, família e galeria → telas placeholder.
  - Horário → tela placeholder de rotinas, com rota própria.
  - Mapa → localização.
  - Mensagens → mural.

## Modelos de dados presentes

- `HomeSummary`: valores de exemplo para nome da família, quantidade de compras
  pendentes, próximo evento, tarefa e aviso.
- `ShoppingItem`: estrutura inicial para item de compra, com identificador,
  nome, categoria, quantidade, estado de compra, autor e estado de sincronização.
- `ShoppingItemEntity`: entidade Room em `data.local`, com tabela
  `shopping_items` e os campos de item informados para a lista familiar.
- `CalendarEvent`: evento com valores padrão vazios para identificador, título,
  descrição, data (`YYYY-MM-DD`), horário (`HH:mm`) e local; o autor padrão é
  “Família”.
- `CalendarEventEntity`: entidade Room correspondente aos eventos do calendário.
- `FamilyDocumentEntity`: metadados de documentos/links da família persistidos
  localmente, com estado de sincronização.
- Eventos, itens de compras e registros de documentos são persistidos com Room;
  os demais valores do resumo da família ainda usam dados de demonstração.

## Agenda familiar

- Implementada tela de calendário com seleção de sete dias próximos, indicação
  da data selecionada e lista de eventos correspondentes ao dia.
- Incluído diálogo para adicionar eventos com título, descrição, horário e
  local; o título é obrigatório.
- Eventos salvos em banco Room/SQLite local e observados reativamente por data.
- Os dados permanecem após fechar o app e são sincronizados com a coleção
  Firestore `calendar_events` ao abrir a tela ou adicionar eventos. Dados
  remotos são reconciliados com o Room, preservando alterações locais pendentes.
- Se a nuvem não estiver disponível, os eventos continuam no dispositivo e a
  sincronização é tentada novamente ao abrir a tela ou adicionar um evento.
- Não há edição, remoção ou recorrência de eventos.

## Lista de compras

- Implementada tela funcional para adicionar itens com nome, categoria e
  quantidade positiva, marcar itens como comprados e removê-los.
- Cada item guarda identificador, categoria, quantidade, estado de compra,
  autor e indicador `isSynced`; o autor usa o nome/e-mail da conta autenticada
  ou “Família” como alternativa.
- A lista é armazenada no Room local, atualizada reativamente e ordenada com
  itens pendentes primeiro. O cartão Listas no painel mostra a quantidade real
  de itens não comprados.
- O repositório sincroniza itens com a coleção Firestore `shopping_items` ao
  abrir a tela e após incluir, atualizar ou remover itens. Itens locais são
  enviados antes de baixar a coleção; alterações locais pendentes prevalecem
  sobre cópias remotas até serem confirmadas.
- Falhas de sincronização são informadas na interface, mantendo as alterações
  no Room para tentativa posterior. Exclusões offline ficam registradas em uma
  tabela de pendências e são enviadas quando a sincronização voltar a funcionar.
- A leitura de reconciliação exige resposta do servidor para evitar apagar dados
  locais usando um snapshot de cache. Não há sincronização periódica em segundo
  plano; uma nova tentativa ocorre ao abrir a tela ou após outra alteração.

## Documentos da família

- Substituída a tela placeholder por um fluxo para cadastrar nome, descrição
  opcional e link HTTP/HTTPS, abrir o link em um aplicativo externo e remover
  registros. O app guarda os metadados e o endereço, não os arquivos.
- A entidade `FamilyDocumentEntity` persiste no Room. O repositório sincroniza
  com a coleção Firestore `family_documents`; inclusões são gravadas localmente
  antes do envio e exclusões offline ficam registradas para nova tentativa.
- A tela sincroniza ao abrir e após alterações. Falhas são apresentadas ao
  usuário e os metadados locais são mantidos.
- Downloads, upload de arquivos, edição de registros e controle de acesso por
  família ainda não foram implementados.

## Persistência local

- Adicionado Room 2.8.4 com KSP 2.3.12 para processar o DAO e a entidade de
  eventos, compras e documentos.
- O banco `atlas_database` passou para a versão 5 e inclui migrações não
  destrutivas desde a versão 1. Migrações criam os dados locais de compras,
  estado de sincronização dos eventos e as tabelas de documentos e exclusões.
- DAOs consultam eventos por data, observam compras e documentos e controlam
  atualizações e exclusões pendentes.
- Repositórios convertem os modelos de domínio e entidades Room. Falhas SQLite
  e de sincronização são apresentadas nas telas correspondentes.

## Firebase e login com Google

- Adicionado o plugin Google Services 4.5.0 e Firebase Authentication e Cloud
  Firestore com Firebase BoM 35.1.0.
- Adicionadas dependências do Credential Manager, integração com Google ID e
  suporte a `Task.await()` com coroutines.
- Adicionado Firebase Analytics usando a versão gerenciada pelo BoM. Nenhum
  evento personalizado do Atlas foi implementado.
- Implementada integração de Cloud Firestore com a coleção `shopping_items`,
  incluindo envio das alterações locais pendentes, download e reconciliação de
  itens, e sincronização de exclusões.
- Implementada sincronização offline-first da Agenda com `calendar_events` e
  dos metadados/links de documentos com `family_documents`.
- Incorporado `app/google-services.json` fornecido pelo usuário para o projeto
  Firebase `casagestpro`.
- Ajustado o `applicationId` para `com.android.application`, correspondente ao
  pacote Android no JSON; o `namespace` de código permanece
  `com.example.atlas`.
- Criada a tela de login Google, com estado de carregamento e apresentação de
  erros da seleção de credencial, validação do token e Firebase Authentication.
- Criado repositório de autenticação para trocar o token Google por credencial
  Firebase; a sessão é observada na atividade principal e há ação para sair.
- Adicionada a permissão de Internet necessária para comunicação com Firebase.
- Para o login real funcionar, o provedor Google precisa estar habilitado no
  Firebase Console e as impressões digitais SHA-1/SHA-256 da chave usada para
  assinar o app precisam estar cadastradas no projeto. A autenticação em
  dispositivo não foi testada.
- É necessário criar o banco Cloud Firestore e configurar suas regras para
  autorizar apenas os usuários previstos. Compras, Agenda e documentos ainda
  não são separados por família, pois o app não possui modelo/identificador
  de família.

## Ambiente Android configurado

- Android SDK instalado no ambiente em `/home/codespace/Android/Sdk`.
- Instalados Android API 35, Android Build Tools 35.0.0, Platform Tools e
  ferramentas de linha de comando.
- Licenças do SDK aceitas para os componentes instalados.
- `local.properties` aponta para o SDK local e está ignorado pelo Git; esse
  caminho é específico deste ambiente e deve ser configurado ao clonar o
  projeto em outra máquina.

## Validação e artefato gerado

- `./gradlew tasks --no-daemon`: concluído com sucesso.
- Dependências de compilação e execução resolvidas pelo Gradle, incluindo
  Firebase Authentication e Analytics.
- O Gradle concluiu a compilação Kotlin após a atualização para Firebase BoM
  35.1.0 e Kotlin 2.4.20.
- `./gradlew assembleDebug --no-daemon`: concluído com sucesso após nova
  execução incremental.
- Após integrar a lista de compras e a migração Room, `./gradlew
  :app:assembleDebug --no-daemon` concluiu com sucesso usando Java 21; KSP e
  compilação Kotlin passaram e o APK de debug foi regenerado.
- Após integrar o Cloud Firestore e a reconciliação de compras, a compilação
  `:app:assembleDebug` passou usando Java 21, um worker e o compilador Kotlin
  in-process. `git diff --check` também passou.
- A integração de Agenda e documentos e as migrações Room correspondentes
  também foram validadas por compilação e geração do APK de debug.
- APK de debug atualizado gerado em `app/build/outputs/apk/debug/app-debug.apk`.
  O pacote é `com.android.application` e suporta
  Android 10 (API 29) ou superior.
- O build registrou avisos de depreciação de alguns ícones do Compose.
- Não foram executados testes automatizados de interface nem instalação/teste
  em dispositivo ou emulador.

## Limitações e próximos passos

- Compras já permite operações locais e sincronização com Firestore. Tarefas,
  mural, finanças e localização continuam sem fluxos completos.
- Horário, aniversários, refeições, receitas, família e galeria têm rotas e
  telas placeholder, mas ainda não têm funcionalidades próprias.
- A configuração OAuth do Firebase (provedor Google e impressões digitais SHA)
  depende do Firebase Console e não foi verificada neste ambiente. Também não há
  notificações nem permissões de localização implementadas. A sincronização de
  compras, eventos e documentos só é tentada ao abrir a tela correspondente ou
  após alterações; ainda não há agendamento em segundo plano.
- Os textos e contagens do painel/modelo são dados de exemplo, não dados reais.
- O APK gerado é de debug e não representa uma versão assinada para publicação.
