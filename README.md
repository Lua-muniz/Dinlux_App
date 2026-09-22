# Dinlux

Aplicativo Android nativo (Kotlin) de gestão financeira pessoal. Permite cadastrar bancos e
cartões, importar extratos bancários (OFX/CSV), criar simulações de compras e economias,
transformar listas de tarefas/compras em lançamentos financeiros, acompanhar dashboards de
gráficos e receber avisos sobre o impacto de compras e economias no saldo projetado.

O backend é o Firebase: **Firebase Authentication** para login/cadastro de usuários e
**Cloud Firestore** para persistência de dados, com cache local persistente habilitado (o app
funciona offline e sincroniza quando a conexão volta).

## Explicação

### Arquitetura e organização do código

O projeto é um módulo Android único (`app`), organizado em pacotes por funcionalidade dentro de
`app/src/main/java/com/luamuniz/dinlux/`:

- `authentication` — login, criação de conta, recuperação de senha (Firebase Auth).
- `core` — utilitários compartilhados (checagem de conectividade, constantes de coleções do
  Firestore, política de privacidade, relógio de testes).
- `excerpt` — importação de extrato bancário (parsers de OFX e CSV, tela de importação e
  histórico de lançamentos).
- `finance` — cadastro de bancos e cartões, saldo, limites.
- `graphics` — gráficos e dashboards embutidos na tela inicial (barras, pizza, linha, donut).
- `home` — tela inicial e tela de finanças.
- `list` — listas de tarefas/compras que podem virar lançamentos financeiros.
- `notifications` — módulo "Avisos": mensagens sobre o impacto de compras/economias no saldo e
  nos limites de cartão, com confirmação/desconfirmação de períodos.
- `profile` — alteração de nome, e-mail e senha, exclusão de conta.
- `simulation` — simulações de compras e economias organizadas em um canvas com nós e grupos.
- `tutorial` — tutorial interativo em slides.

Cada módulo segue o mesmo padrão: uma `Activity` (camada de visão), um `Repository` que fala
diretamente com o Firestore/Firebase Auth, e, quando necessário, um `ViewModel` para estado de
UI.

### Backend

Não há servidor próprio: toda a persistência é feita direto no Cloud Firestore através do SDK do
Firebase, e a autenticação é feita pelo Firebase Authentication. Isso significa que rodar o app
localmente exige um arquivo de configuração do Firebase (`google-services.json`), descrito nos
pré-requisitos abaixo.

## Pré-requisitos

- **Android Studio** atualizado, na versão estável mais recente disponível (o projeto usa Android
  Gradle Plugin 9.2.1, Gradle 9.4.1 e `compileSdk = 37`, então é necessária uma versão do Android
  Studio compatível com essas ferramentas; versões desatualizadas vão recusar sincronizar o
  projeto ou pedir para atualizar o plugin).
- Não é necessário instalar um JDK separado: o Android Studio já vem com um JDK embutido (JBR),
  usado por padrão pelo Gradle.
- **Git** instalado.
- Uma conexão com a internet (para o Gradle baixar dependências na primeira sincronização e para
  o app se conectar ao Firebase).
- O arquivo **`google-services.json`** do projeto Firebase "dinlux", para o pacote
  `com.luamuniz.dinlux`. Esse arquivo não fica no repositório (é ignorado pelo Git de propósito,
  por conter identificadores do projeto Firebase) e precisa ser obtido separadamente com quem
  administra o projeto Firebase, ou gerado em um projeto Firebase próprio (nesse caso os dados
  salvos serão isolados nesse novo projeto, sem acesso aos dados já existentes).
- Para testar em um dispositivo físico: um aparelho Android com **depuração USB** habilitada
  (Configurações > Sobre o telefone > tocar 7 vezes em "Número da versão" para habilitar as
  opções de desenvolvedor, depois Configurações > Opções do desenvolvedor > Depuração USB). Como
  alternativa, um emulador Android configurado pelo próprio Android Studio (AVD Manager).

## Execução no Windows

1. Baixe e instale o Android Studio a partir do site oficial
   (`https://developer.android.com/studio`), seguindo o instalador padrão (Next/Next/Finish). O
   instalador já deixa o SDK Manager e um emulador básico configurados.
2. Clone o repositório (via `git clone`, usando a URL HTTPS ou SSH do repositório) ou baixe o
   `.zip` do código e extraia em uma pasta local, por exemplo `C:\Projetos\Dinlux`.
3. Abra o Android Studio e escolha **Open** (ou **File > Open**, se o Android Studio já estiver
   aberto em outro projeto), navegue até a pasta onde o projeto foi clonado/extraído e selecione
   essa pasta (a que contém o arquivo `settings.gradle.kts` na raiz).
4. Aguarde a sincronização automática do Gradle (barra de progresso na parte inferior da janela,
   "Gradle Sync"). Na primeira vez, isso pode demorar alguns minutos, pois o Gradle baixa o
   wrapper, o Android Gradle Plugin e as dependências do projeto. Se o Android Studio avisar que
   faltam componentes do SDK (por exemplo, a Platform do `compileSdk` configurado), aceite a
   instalação sugerida pelo próprio IDE.
5. Copie o arquivo `google-services.json` obtido conforme os pré-requisitos para dentro da pasta
   `app/` do projeto (o caminho final deve ser `app/google-services.json`, no mesmo nível de
   `app/build.gradle.kts`). Sem esse arquivo, a sincronização do Gradle falha, porque o plugin
   `com.google.gms.google-services` exige esse arquivo para gerar as configurações do Firebase.
6. Sincronize o Gradle novamente caso o arquivo tenha sido adicionado depois da primeira
   sincronização (ícone do elefante com uma seta, "Sync Project with Gradle Files", na barra de
   ferramentas).
7. Configure um destino de execução:
   - **Emulador**: abra o AVD Manager (ícone de celular na barra de ferramentas, ou
     Tools > Device Manager), crie um dispositivo virtual (recomenda-se uma imagem de sistema
     com Google Play/Google APIs, API compatível com `minSdk = 26` ou superior) e aguarde o
     download da imagem do sistema.
   - **Dispositivo físico**: conecte o aparelho ao computador via USB com a depuração USB
     habilitada (ver pré-requisitos); ao conectar, o Windows pode pedir para instalar drivers
     USB do fabricante do aparelho, e o próprio aparelho vai pedir para autorizar a depuração
     nesse computador (aceite o diálogo que aparece na tela do celular).
8. Com o dispositivo/emulador selecionado no seletor de dispositivos (canto superior, ao lado do
   nome da configuração de execução "app"), clique no botão verde de play (Run 'app') ou use o
   atalho Shift+F10.
9. O Android Studio compila o app, instala no dispositivo/emulador selecionado e abre
   automaticamente. A primeira tela exibida é a de login; use "Criar Conta" para cadastrar um
   usuário novo (ele fica salvo no Firebase Authentication do projeto configurado no
   `google-services.json`) ou entre com uma conta já existente nesse mesmo projeto Firebase.

## Execução no Linux

1. Baixe o Android Studio para Linux em `https://developer.android.com/studio` (arquivo
   `.tar.gz`) ou instale via Snap: `sudo snap install android-studio --classic`.
   - Se optar pelo `.tar.gz`: extraia o conteúdo (por exemplo, em `~/android-studio`) e execute
     `~/android-studio/bin/studio.sh` para abrir o assistente de instalação inicial (baixa o SDK,
     ferramentas de build etc.).
2. Habilite a aceleração de hardware para o emulador (opcional, mas recomendado caso pretenda
   usar um emulador em vez de um dispositivo físico):
   ```bash
   sudo apt install qemu-kvm
   sudo usermod -aG kvm $USER
   ```
   É necessário reiniciar a sessão (logout/login) para o usuário passar a fazer parte do grupo
   `kvm`. Verifique com `kvm-ok` (pacote `cpu-checker`) se a virtualização está disponível.
3. Clone o repositório:
   ```bash
   git clone <URL-do-repositorio>
   cd Dinlux
   ```
4. Abra o Android Studio, escolha **Open** e selecione a pasta clonada (a que contém
   `settings.gradle.kts`).
5. Aguarde a sincronização do Gradle, exatamente como descrito no passo 4 da seção do Windows.
6. Copie o `google-services.json` para `app/google-services.json`, como descrito no passo 5 da
   seção do Windows, e sincronize o Gradle novamente se necessário.
7. Configure o destino de execução:
   - **Emulador**: Tools > Device Manager > criar um dispositivo virtual (mesmas recomendações
     da seção do Windows).
   - **Dispositivo físico via USB**: no Linux normalmente é necessário configurar regras
     `udev` para o `adb` enxergar o aparelho sem precisar de `sudo`. Um caminho comum:
     ```bash
     sudo apt install android-sdk-platform-tools-common
     ```
     Esse pacote já traz um conjunto padrão de regras `udev` para a maioria dos fabricantes.
     Depois de plugar o cabo USB, rode `adb devices` (o executável `adb` fica em
     `<pasta-do-sdk>/platform-tools/adb`, geralmente `~/Android/Sdk/platform-tools/adb`) e
     autorize a depuração no diálogo que aparece na tela do aparelho. Se o dispositivo aparecer
     como `unauthorized` ou não aparecer, confira se a depuração USB está habilitada no aparelho
     e tente reconectar o cabo.
8. Selecione o dispositivo/emulador e clique em Run 'app' (ou Shift+F10), da mesma forma descrita
   no passo 8 da seção do Windows.

## Estrutura esperada na IDE

Depois da sincronização, a árvore do projeto (visão "Android" no painel lateral do Android
Studio) deve mostrar:

- `app/manifests/AndroidManifest.xml`
- `app/java/com.luamuniz.dinlux` com os pacotes listados na seção Explicação
- `app/res` com as subpastas `layout`, `drawable`, `values`, `mipmap-*` e `xml`
- `Gradle Scripts` com `build.gradle.kts` (Project e Module), `settings.gradle.kts` e
  `gradle.properties`

Se algum desses itens não aparecer, ou aparecer com um ícone de erro, geralmente é sinal de que a
sincronização do Gradle ainda não terminou ou falhou (ver o painel "Build" na parte inferior da
janela para a mensagem de erro exata).
