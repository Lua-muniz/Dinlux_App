package com.luamuniz.dinlux.tutorial

import com.luamuniz.dinlux.R

/**
 * Slide dentro da explicação de uma seção do Tutorial
 */
data class TutorialSlide(
    val imageRes: Int?,
    val title: String,
    val text: String,
    val imageRes2: Int? = null,
    val blocks: List<TutorialSlideBlock>? = null
)

data class TutorialSlideBlock(val imageRes: Int?, val text: String)

object TutorialContent {

    val SECOES: Map<String, List<TutorialSlide>> = mapOf(
        "menu_perfil" to listOf(
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_menu_perfil_painel,
                imageRes2 = R.drawable.tutorial_slide_menu_perfil_painel_2,
                title = "Menu de Perfil",
                text = "O painel de Menu de Perfil abre pela lateral esquerda da tela " +
                    "principal, tocando no seu nome ou no ícone de perfil no topo da " +
                    "Home. Nele você encontra sete opções para gerenciar sua conta: " +
                    "Alterar Nome, Alterar Email, Alterar Senha, Políticas de " +
                    "Privacidade, Exportar Meus Dados, Sair do Perfil e Excluir Conta. " +
                    "Deslize para o lado para ver o que cada uma faz."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Alterar Nome",
                text = "Troca o nome usado na saudação do app, mostrada como Olá, " +
                    "seguido do seu nome, no topo do próprio painel. Basta digitar o " +
                    "novo nome e salvar, sem precisar confirmar com a senha. A " +
                    "alteração aparece imediatamente em todo o app."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Alterar Email",
                text = "Troca o email usado para entrar no aplicativo. Você digita o " +
                    "novo email e confirma com a sua senha atual, para garantir que é " +
                    "mesmo você. Depois de salvar, o Firebase envia um link de " +
                    "confirmação para o novo email: a troca só se efetiva de verdade " +
                    "quando esse link é aberto. Enquanto o link não é confirmado, o " +
                    "login continua funcionando normalmente com o email antigo."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Alterar Senha",
                text = "Troca a senha de acesso à conta. É preciso informar a senha " +
                    "atual, a nova senha e confirmar a nova senha novamente. A nova " +
                    "senha precisa ter pelo menos seis caracteres e ser diferente da " +
                    "atual. Sem saber a senha atual não é possível trocar, mesmo " +
                    "estando logado no aplicativo."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Políticas de Privacidade",
                text = "Abre a qualquer momento o texto completo do Termo de " +
                    "Privacidade que você aceitou ao criar a conta, mostrando também a " +
                    "data e a hora exatas em que você aceitou. É o mesmo termo " +
                    "apresentado na criação da conta, sempre disponível para consulta."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Exportar Meus Dados",
                text = "Gera um arquivo com todos os seus dados salvos no Dinlux: " +
                    "bancos, cartões, simulações, lançamentos, extrato importado e " +
                    "listas. Você escolhe entre exportar em JSON, um arquivo técnico " +
                    "que guarda os dados em formato bruto, ou em PDF, um relatório já " +
                    "organizado e pronto para ler ou imprimir. Depois de escolher o " +
                    "formato, você escolhe também onde salvar o arquivo no seu " +
                    "aparelho."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Sair do Perfil",
                text = "Encerra a sessão atual e leva de volta à tela de login. Seus " +
                    "dados continuam salvos normalmente na sua conta. Basta entrar de " +
                    "novo com email e senha para continuar de onde parou."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Excluir Conta",
                text = "Apaga permanentemente a conta e todos os dados associados a " +
                    "ela: bancos, cartões, simulações, lançamentos, extratos " +
                    "importados, listas e o próprio aceite do Termo de Privacidade. É " +
                    "preciso confirmar com a senha atual antes de excluir. Essa ação " +
                    "não pode ser desfeita: depois de excluída, não é possível " +
                    "recuperar a conta nem nenhum dos dados."
            )
        ),
        "listas" to listOf(
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_listas_tela,
                title = "Onde Ficam suas Listas",
                text = "Essa é a tela Listas, aberta pelo botão Listas da Home. Aqui " +
                    "ficam todas as listas que você cria, cada uma como um bloco de " +
                    "notas separado: lista de compras, lista de tarefas, o que você " +
                    "quiser. Para criar uma lista nova, toque no símbolo de mais no " +
                    "canto superior direito e digite um nome. Toque em qualquer lista " +
                    "para abrir os itens dela."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_listas_opcoes,
                title = "Opções da Lista",
                text = "Tocando nos três pontinhos ao lado de uma lista aparecem três " +
                    "opções. Renomear troca só o nome da lista, sem mexer nos itens. " +
                    "Excluir apaga a lista inteira e todos os seus itens de uma vez, " +
                    "sem volta. Lançar desconta o valor já Feito da lista, ou seja, a " +
                    "soma dos itens com preço que você já marcou como concluídos, de " +
                    "um banco ou cartão à sua escolha, mostrando quanto você tem " +
                    "disponível em cada um; a lista continua existindo depois, pronta " +
                    "pra ser reaproveitada."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_listas_itens,
                title = "Itens da Lista",
                text = "Ao abrir uma lista você vê os itens dela. Toque no símbolo de " +
                    "mais para adicionar um item novo: só o nome é obrigatório, " +
                    "quantidade e preço são opcionais. No topo aparecem duas linhas de " +
                    "resumo. Itens mostra quantos itens já foram marcados de quantos " +
                    "existem no total, e aparece mesmo em listas sem preço nenhum, " +
                    "como uma lista de tarefas comum. A segunda linha, com Total, " +
                    "Feito e Falta, só aparece quando pelo menos um item tem preço: " +
                    "Total é a soma de preço vezes quantidade de todos os itens com " +
                    "preço, Feito é essa mesma soma só dos itens já marcados, e Falta " +
                    "é a diferença entre os dois. Qtd é a quantidade de cada item, " +
                    "usada junto com o preço para calcular o valor daquela linha, como " +
                    "em Qtd: 8 vezes R$ 2,50 igual a R$ 20,00."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_listas_marcando,
                title = "Marcando um Item",
                text = "Tocar na caixinha ao lado de um item marca ou desmarca ele na " +
                    "hora, sem pedir confirmação, já que aqui não mexe em saldo real, " +
                    "é só um checklist. Assim que você marca um item, o contador " +
                    "Itens sobe, aqui foi de 0 de 4 para 1 de 4, e se o item tem " +
                    "preço, o valor dele entra na conta de Feito e sai da conta de " +
                    "Falta. No exemplo, marcar o Sabonete, de R$ 20,00, fez o Feito " +
                    "subir de R$ 0,00 para R$ 20,00 e a Falta cair de R$ 29,00 para " +
                    "R$ 9,00."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_listas_valor,
                title = "Item com Valor e sem Valor",
                text = "Nem todo item precisa ter preço. Aqui Sabonete e Óleo têm " +
                    "preço, e Sacar Dinheiro não tem. Marcando os três, o contador " +
                    "Itens sobe normalmente para os três, chegando a 3 de 4, mas o " +
                    "Feito só aumentou com Sabonete e Óleo: o Feito bateu exatamente " +
                    "com o Total, R$ 29,00, porque Sacar Dinheiro não tem preço para " +
                    "somar em lugar nenhum. Ou seja, um item sem preço conta como " +
                    "tarefa concluída no contador Itens, mas não entra na conta de " +
                    "dinheiro."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_listas_item_opcoes,
                title = "Opções do Item",
                text = "Tocar em qualquer lugar do item, fora da caixinha de marcar, " +
                    "abre duas opções. Editar reabre o mesmo formulário de criação, " +
                    "já preenchido, para trocar nome, quantidade ou preço. Excluir " +
                    "remove o item da lista, sem volta."
            )
        ),
        "importar_extrato" to listOf(
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_importar_extrato_bancos,
                title = "Escolha o Banco",
                text = "Essa tela abre pelo botão Inserir Extrato da Home e mostra " +
                    "todos os bancos que você já cadastrou. Escolher o banco aqui antes " +
                    "de importar é o que garante que o extrato certo vai para o banco " +
                    "certo, sem risco de misturar o extrato de um banco com o outro."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_importar_extrato_arquivo,
                title = "Selecionar Arquivo",
                text = "Depois de escolher o banco, toque em Selecionar Arquivo. Antes " +
                    "de abrir o seletor, o app pergunta o saldo atual desse banco, " +
                    "visto no aplicativo ou site dele, e atualiza esse valor na hora. " +
                    "Só depois disso o seletor de arquivo abre, aceitando extrato " +
                    "exportado em formato OFX ou CSV. OFX é o formato mais confiável e " +
                    "identifica o banco sozinho; CSV depende do layout de cada banco e " +
                    "pode não ser reconhecido em todos os casos. Depois de escolher o " +
                    "arquivo, você ainda vê um resumo de tudo o que foi encontrado " +
                    "antes de confirmar e salvar."
            )
        ),
        "financas" to listOf(
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_financas_tela,
                title = "Finanças",
                text = "Essa é a tela Finanças, aberta pelo botão Finanças da Home, " +
                    "mostrando um banco de cada vez. O nome do banco aparece no topo, " +
                    "junto com o ícone dele, e as setas dos lados trocam para o " +
                    "próximo banco ou o anterior, quando você tem mais de um " +
                    "cadastrado. Logo abaixo fica o Saldo. O card ciano mostra o " +
                    "cartão de crédito desse banco, com a bandeira em destaque, o " +
                    "limite, o dia de fechamento e o dia de vencimento; o número no " +
                    "canto inferior, como 1/2, indica qual cartão está sendo mostrado " +
                    "e quantos esse banco tem ao todo, e as setas dos lados do card " +
                    "trocam entre eles quando há mais de um. Abaixo do card fica o " +
                    "Extrato, mostrando a tabela de Data, Valor e Descrição de tudo o " +
                    "que já foi importado pela tela Importar Extrato para esse banco, " +
                    "ou a mensagem Nenhum lançamento importado ainda enquanto nada " +
                    "foi importado."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_financas_editar_saldo,
                title = "Editar Saldo",
                text = "Tocar no valor do Saldo abre esse diálogo. Digite o valor " +
                    "atual do banco e toque em Salvar para atualizar na hora."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_financas_opcoes_banco,
                title = "Opções do Banco",
                text = "Tocar no nome do banco abre essas quatro opções. Renomear " +
                    "troca só o nome do banco. Criar leva para o cadastro de um " +
                    "banco novo. Excluir Extrato apaga todo o extrato já importado " +
                    "para esse banco, sem mexer no banco, nos cartões ou nas " +
                    "simulações dele, útil quando um extrato errado foi importado ou " +
                    "você não quer mais aquele extrato salvo. Excluir apaga esse " +
                    "banco e tudo o que está ligado a ele, como os cartões dele, os " +
                    "lançamentos e Grupos que o usam em Simulações e o extrato " +
                    "importado, sem volta."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_financas_novo_banco,
                title = "Novo Banco",
                text = "Esse formulário abre ao criar um banco novo. Nome identifica " +
                    "o banco, e Débito atual é o saldo dele no momento do cadastro. " +
                    "A seção Cartões de crédito é opcional: toque em mais Adicionar " +
                    "Cartão para incluir um ou mais cartões já no cadastro do banco, " +
                    "preenchendo bandeira, limite, dia de fechamento, dia de " +
                    "vencimento e, se cobrar juros no parcelamento, a taxa de juros " +
                    "mensal. Você também pode deixar essa seção vazia e cadastrar os " +
                    "cartões depois, direto na tela Finanças. Ao final, toque em " +
                    "Salvar Banco."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_financas_opcoes_cartao,
                title = "Opções do Cartão",
                text = "Tocar nos três pontinhos no canto do card do cartão abre " +
                    "essas quatro opções. Editar reabre o formulário desse cartão já " +
                    "preenchido, para trocar bandeira, limite, fechamento, " +
                    "vencimento ou juros. Criar leva para o cadastro de um cartão " +
                    "novo para esse mesmo banco. Limite Disponível corrige na hora o " +
                    "limite disponível desse cartão, útil quando uma compra foi " +
                    "feita no cartão sem ser registrada aqui no aplicativo. Excluir " +
                    "apaga o cartão e tudo relacionado a ele, como as compras e " +
                    "parcelas dele já lançadas em Simulações, sem volta."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_financas_novo_cartao,
                title = "Novo Cartão",
                text = "Esse formulário abre ao criar ou editar um cartão. Bandeira " +
                    "escolhe a bandeira do cartão; se não estiver na lista, escolha " +
                    "Outro e digite o nome. Limite é o limite total de crédito do " +
                    "cartão. Dia fechamento e Dia vencimento são os dias do mês, de " +
                    "1 a 31, em que a fatura fecha e vence. Se o cartão cobra juros " +
                    "no parcelamento, ative Cobra juros no parcelamento e informe a " +
                    "Taxa de juros mensal. Ao final, toque em Salvar Cartão."
            )
        ),
        "simulacoes" to listOf(
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_simulacoes_lista,
                title = "Simulação",
                text = "Essa é a tela Simulação, aberta pelo botão Simulações da Home, " +
                    "mostrando todas as suas simulações, separadas em Ativas e " +
                    "Inativas. Toque em qualquer uma para abrir o canvas dela. O " +
                    "símbolo de mais no canto superior direito cria uma simulação " +
                    "nova, só pedindo um nome para começar."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Opções da Simulação",
                text = "",
                blocks = listOf(
                    TutorialSlideBlock(
                        imageRes = R.drawable.tutorial_slide_simulacoes_opcoes_inativa,
                        text = "Tocar nos três pontinhos de uma simulação abre essas " +
                            "opções. Renomear troca só o nome dela. Excluir apaga a " +
                            "simulação inteira, com todos os seus lançamentos e " +
                            "Grupos, sem volta. Numa simulação ainda inativa, a " +
                            "terceira opção é Ativar: ela recalcula a data de cada " +
                            "lançamento a partir de hoje, mantendo parcelas, meses e " +
                            "juros, e passa a ser acompanhada de verdade pelo módulo " +
                            "de Avisos."
                    ),
                    TutorialSlideBlock(
                        imageRes = R.drawable.tutorial_slide_simulacoes_opcoes_ativa,
                        text = "Numa simulação já ativa, a terceira opção vira " +
                            "Desativar. Desativar não muda nada nos lançamentos, só " +
                            "para de ser acompanhada em Avisos; você pode ativar de " +
                            "novo quando quiser."
                    )
                )
            ),
            TutorialSlide(
                imageRes = null,
                title = "Adicionando uma Compra",
                text = "",
                blocks = listOf(
                    TutorialSlideBlock(
                        imageRes = R.drawable.tutorial_slide_simulacoes_canvas_vazio,
                        text = "Esse é o canvas de uma simulação, onde ficam os " +
                            "lançamentos dela. O botão com o cifrão no topo abre a " +
                            "legenda de cores, mostrando cada banco e cartão " +
                            "cadastrado e a cor que representa ele nos nós. O " +
                            "símbolo de mais no canto inferior direito abre a " +
                            "escolha entre Compra e Economia."
                    ),
                    TutorialSlideBlock(
                        imageRes = R.drawable.tutorial_slide_simulacoes_nova_compra,
                        text = "Ao escolher Compra, esse formulário abre. Nome " +
                            "identifica a compra, Valor total é o valor dela, Banco " +
                            "escolhe de qual banco ela sai, e Débito ou Crédito " +
                            "escolhe a forma de pagamento. Em Débito, o último " +
                            "campo é a quantidade de meses até você terminar de " +
                            "pagar; em Crédito, é a quantidade de parcelas."
                    )
                )
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_simulacoes_nova_compra_credito,
                title = "Comprando no Crédito",
                text = "Escolhendo Crédito, aparece também o campo Cartão, para " +
                    "escolher qual cartão desse banco vai ser usado, e, se esse " +
                    "cartão cobrar juros no parcelamento, um aviso mostrando a taxa " +
                    "e a partir de quantas parcelas ela passa a valer."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_simulacoes_nova_economia,
                title = "Criando uma Economia",
                text = "Escolhendo Economia no lugar de Compra, esse outro " +
                    "formulário abre. Nome identifica a meta, Valor da meta é " +
                    "quanto você quer juntar, Banco (débito) escolhe de qual banco " +
                    "o valor vai sair todo mês, e Data de início e Data de fim " +
                    "definem o período; o app divide o valor da meta por esse " +
                    "período para calcular quanto guardar por mês."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Os Nós no Canvas",
                text = "",
                blocks = listOf(
                    TutorialSlideBlock(
                        imageRes = R.drawable.tutorial_slide_simulacoes_nos_grupos,
                        text = "Cada compra ou economia que você cria vira um nó no " +
                            "canvas, com a cor do banco ou cartão dela, igual na " +
                            "legenda. Nós do mesmo banco e do mesmo cartão se juntam " +
                            "automaticamente num mesmo Grupo, ligados por linhas " +
                            "entre si."
                    ),
                    TutorialSlideBlock(
                        imageRes = R.drawable.tutorial_slide_simulacoes_detalhe_no,
                        text = "Tocar em um nó abre os detalhes dele. Numa economia " +
                            "aparecem a meta, o valor mensal, o banco, as datas de " +
                            "início e fim, e uma barra mostrando quanto já foi " +
                            "guardado; numa compra aparecem o valor total, a forma " +
                            "de pagamento, as parcelas, o total com juros e o " +
                            "período. Editar reabre o formulário já preenchido, " +
                            "Excluir apaga o lançamento, e Fechar só fecha os " +
                            "detalhes."
                    ),
                    TutorialSlideBlock(
                        imageRes = R.drawable.tutorial_slide_simulacoes_grupo_cheio,
                        text = "Um Grupo pode ter no máximo seis nós, todos ligados " +
                            "entre si como nesse exemplo. Ao chegar nesse limite, " +
                            "nenhum outro lançamento daquele banco e cartão entra " +
                            "nesse Grupo; um novo Grupo é criado automaticamente " +
                            "para os próximos."
                    )
                )
            ),
            TutorialSlide(
                imageRes = null,
                title = "Renomeando um Grupo",
                text = "Cada Grupo começa com um nome automático, como Grupo 1 ou " +
                    "Grupo 2, só como ponto de partida. Tocar no nome do Grupo no " +
                    "canvas abre um campo para trocar por qualquer nome que fizer " +
                    "mais sentido para você, como o nome de uma viagem ou de um " +
                    "projeto."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_simulacoes_mover_no,
                title = "Movendo um Nó entre Grupos",
                text = "Você pode arrastar um nó e soltar sobre outro Grupo para " +
                    "mudar ele de lugar, como o nó Colar sendo arrastado nessa " +
                    "imagem. Isso só funciona se o Grupo de destino for do mesmo " +
                    "banco e do mesmo cartão do nó, e ainda tiver vaga, respeitando " +
                    "o limite de seis nós; soltando sobre um Grupo incompatível ou " +
                    "já cheio, nada acontece."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Zoom e Navegação no Canvas",
                text = "O canvas aceita zoom e arrastar como qualquer mapa: " +
                    "belisque com dois dedos para aproximar ou afastar a visão, e " +
                    "arraste com um dedo para navegar entre os Grupos quando a " +
                    "simulação tiver muitos lançamentos."
            )
        ),
        "avisos" to listOf(
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_avisos_lista,
                title = "Avisos",
                text = "Essa é a tela Avisos, aberta pelo sino no topo da Home. " +
                    "Aparece um item para cada banco que tem pelo menos uma compra " +
                    "ou economia de uma simulação ativa ainda não excluída daqui. " +
                    "O número ao lado de um banco mostra quantas mensagens você " +
                    "ainda não viu, e some quando não há nenhuma pendência nova; " +
                    "toque em um banco para abrir o histórico dele."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_avisos_chat_confirmada,
                title = "O Chat de um Banco",
                text = "Dentro do chat, o Saldo no topo mostra o saldo atual " +
                    "daquele banco. Cada compra ou economia vira uma sessão " +
                    "própria, com o nome dela como cabeçalho; toque na seta ao " +
                    "lado do nome para recolher ou expandir as mensagens dela. " +
                    "Dentro de cada sessão, uma mensagem por mês (economia) ou " +
                    "por parcela (compra) pergunta se aquele valor foi guardado " +
                    "ou pago; marcar a caixinha confirma na hora."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Quando um Aviso Aparece",
                text = "Numa economia, aparece uma mensagem para cada mês do " +
                    "período dela que já chegou, esteja confirmado ou não; meses " +
                    "futuros ainda não aparecem. Numa compra, aparece uma " +
                    "mensagem para cada parcela cujo ciclo já chegou: no " +
                    "crédito, o ciclo segue o dia de vencimento cadastrado no " +
                    "cartão; no débito, segue o próprio dia do mês em que a " +
                    "compra foi feita. Uma compra ou economia sem nenhum mês ou " +
                    "ciclo ainda decorrido não aparece em Avisos até o primeiro " +
                    "chegar."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_avisos_chat_atrasada,
                title = "Confirmada, Atrasada ou Pendente",
                text = "Cada mensagem mostra um destes três estados: Confirmada, " +
                    "em verde, quando a caixinha está marcada; Atrasada, em " +
                    "vermelho, quando o mês ou a parcela já passou e ainda não " +
                    "foi confirmada; e Pendente, em branco, quando o mês ou a " +
                    "parcela é do período atual e ainda não foi confirmada. " +
                    "Qualquer mensagem pode ser marcada ou desmarcada a qualquer " +
                    "momento, em qualquer ordem."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Descontos e Devoluções no Saldo e no Limite",
                text = "Confirmar uma mensagem tem efeito imediato de verdade: " +
                    "numa economia, desconta o valor mensal do saldo do banco; " +
                    "numa compra no débito, desconta o valor da parcela do " +
                    "saldo do banco; numa compra no crédito, desconta o valor " +
                    "da parcela do saldo do banco e também libera esse mesmo " +
                    "valor no limite disponível do cartão, mostrado na aba " +
                    "Cartões dos Gráficos. Desmarcar reverte exatamente o " +
                    "oposto, devolvendo o valor ao saldo (e ocupando de novo o " +
                    "limite, no caso do crédito); por mexer com dinheiro de " +
                    "verdade, desmarcar sempre pede uma confirmação antes de " +
                    "aplicar."
            ),
            TutorialSlide(
                imageRes = null,
                title = "Saldo ao Sair do Chat",
                text = "O Saldo mostrado no topo do chat já reflete, em tempo " +
                    "real, cada confirmação ou desconfirmação feita durante " +
                    "aquela visita. Se você sair do chat com o saldo negativo " +
                    "depois de confirmar algo, o app pergunta qual é o seu " +
                    "saldo atual de verdade: Atualizar corrige o saldo mantendo " +
                    "tudo o que foi confirmado, e Fechar desfaz todas as " +
                    "confirmações feitas naquela visita, como se você não " +
                    "tivesse entrado."
            )
        ),
        "graficos" to listOf(
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_graficos_simulacoes,
                title = "Gráficos",
                text = "O dashboard de Gráficos fica embutido na própria Home, " +
                    "com três abas: Simulações, Movimentações e Cartões. Na aba " +
                    "Simulações, cada simulação ativa ganha uma seção com um " +
                    "grupo de colunas para cada banco ou cartão usado nela, " +
                    "como nessa imagem. Cada coluna é uma compra ou uma " +
                    "economia, com a altura mostrando o quanto já foi " +
                    "concluído; um contorno verde ao redor da coluna marca uma " +
                    "economia, para diferenciar de uma compra quando as duas " +
                    "têm a mesma cor do banco ou cartão."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_graficos_progresso_geral,
                title = "Progresso Geral",
                text = "No topo da aba Simulações fica o Progresso Geral, uma " +
                    "linha com um ponto para cada simulação ativa. A posição de " +
                    "cada ponto é a média de conclusão de todos os lançamentos " +
                    "daquela simulação, contando cada um igual, sem pesar pelo " +
                    "valor em R$; ligando os pontos dá para comparar o " +
                    "andamento de uma simulação inteira com o das outras."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_graficos_movimentacoes,
                title = "Aba Movimentações",
                text = "A aba Movimentações mostra uma pizza para cada banco " +
                    "cadastrado, mesmo os que ainda não têm nenhum extrato " +
                    "importado. Verde é o total de entrada e vermelho o total " +
                    "de saída, somando todos os extratos já importados para " +
                    "aquele banco, as mesmas cores já usadas na tabela de " +
                    "Extrato em Finanças. Os bancos aparecem ordenados pelo " +
                    "maior volume movimentado primeiro."
            ),
            TutorialSlide(
                imageRes = R.drawable.tutorial_slide_graficos_cartoes,
                title = "Aba Cartões",
                text = "A aba Cartões mostra uma rosca para cada cartão de " +
                    "crédito cadastrado, com o fechamento e o vencimento dele " +
                    "ao lado. Vermelho é o limite já usado e verde é o limite " +
                    "disponível, a mesma conta usada em Finanças; um cartão " +
                    "sem nenhuma parcela em aberto aparece com a rosca inteira " +
                    "verde."
            )
        )
    )
}
