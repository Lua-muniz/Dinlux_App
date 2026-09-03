package com.luamuniz.dinlux.core

/**
 * Texto dos Termos de Privacidade do Dinlux
 */
object TermosDePrivacidade {

    const val VERSAO = "1.0"

    val TEXTO = """
        TERMOS DE PRIVACIDADE DINLUX
        Versão 1.0, última atualização: 30/08/2026

        Este documento explica, de forma detalhada, quais dados o Dinlux coleta, para que
        eles são usados, onde ficam guardados, como são protegidos e quais direitos você
        tem sobre eles. O Dinlux é um aplicativo de gestão financeira pessoal desenvolvido
        como projeto acadêmico (Trabalho de Conclusão de Curso), e segue os princípios e
        obrigações da Lei Geral de Proteção de Dados (Lei nº 13.709/2018, a LGPD) desde o
        design do sistema, não como um adendo de última hora.

        Ao marcar a caixa de confirmação na tela de criação de conta, você declara que leu
        e concorda com este termo. Esse aceite é obrigatório: sem ele, a conta não é
        criada. O fato de você ter aceitado, com a data, hora e a versão exata deste
        texto, fica registrado e guardado na sua conta, como exige a LGPD (é preciso não
        só obter o consentimento, mas conseguir comprovar que ele foi dado).


        1. QUEM SOMOS E COMO FALAR COM A GENTE

        O Dinlux foi desenvolvido por Luana Barros Muniz, aluna do curso de Ciência da
        Computação da Universidade Estadual de Mato Grosso do Sul (UEMS), unidade de
        Dourados, Brasil, como Trabalho de Conclusão de Curso (TCC), sob orientação do
        Professor Mestre André Chastel Lima. Dúvidas, pedidos relacionados aos seus dados
        (acesso, correção, exclusão) ou qualquer questão sobre este termo podem ser feitos
        pelo e-mail dinluxgestaofinanceira@gmail.com, canal oficial de contato e suporte
        do aplicativo.


        2. QUAIS DADOS SÃO COLETADOS E PARA QUE SERVEM

        2.1. Dados de identificação e acesso
        Nome, e-mail e senha, coletados na criação da conta. O e-mail e a senha são usados
        exclusivamente para autenticação (login) através do Firebase Authentication, serviço
        do Google. A senha nunca é vista nem guardada pelo Dinlux em texto puro: ela é
        processada inteiramente pela infraestrutura do Firebase, que aplica hash e salt
        (uma transformação criptográfica que impede a recuperação da senha original, mesmo
        em caso de acesso indevido ao banco de dados). O nome é usado apenas para
        personalizar a saudação dentro do app ("Olá, [nome]").

        2.2. Dados financeiros informados voluntariamente por você
        O Dinlux não se conecta ao seu banco de verdade, não usa Open Banking, não pede
        usuário e senha do seu banco e não tem acesso a nenhuma informação bancária real
        automaticamente. TUDO que o app sabe sobre suas finanças é o que você mesmo digita
        ou importa manualmente: o saldo declarado de cada banco cadastrado e o limite, o
        dia de fechamento, o dia de vencimento e a taxa de juros de cada cartão cadastrado;
        as simulações de compras parceladas e metas de economia que você cria dentro do
        app; e os extratos bancários (arquivos OFX ou CSV) que você exporta do site ou do
        app do seu banco e importa manualmente no Dinlux.

        Finalidade do saldo e dos dados de cartão (débito e crédito): usados EXCLUSIVAMENTE
        para as funcionalidades de Simulação do app, ou seja, para simular o impacto de
        uma compra parcelada ou de uma meta de economia no seu orçamento. Na prática, isso
        significa: o app usa o saldo declarado e o limite do cartão pra calcular quanto
        ainda está disponível; usa o dia de fechamento e vencimento do cartão pra calcular
        em qual fatura cada parcela cai; e, quando a compra é no crédito, aplica a taxa de
        juros informada (Tabela Price) a partir da 2ª parcela pra calcular o valor de cada
        parcela com juros. Nenhum desses dados sai do seu controle: o saldo só muda de
        verdade quando você mesmo confirma uma parcela ou economia no módulo de Avisos, ou
        quando você atualiza o saldo manualmente. O app nunca movimenta dinheiro nem se
        comunica com nenhuma instituição financeira real.

        Finalidade do extrato bancário importado: usado APENAS para alimentar as tabelas e
        os gráficos de "Movimentações" dentro do app, pra você ter uma visão organizada do
        que entrou e saiu da sua conta no período importado. O extrato importado não altera
        seu saldo automaticamente, não é usado em nenhum cálculo de simulação, e não é
        compartilhado com ninguém, serve só de consulta e visualização pra você mesmo.

        2.3. Dados de uso do app
        Itens de listas (por exemplo, lista de mercado), textos e valores que você cadastra
        nesses módulos, usados só para exibir essas listas de volta pra você, sem nenhuma
        outra finalidade.

        2.4. O que o Dinlux NÃO coleta
        O app não pede CPF, não usa login social (Google ou Facebook), não acessa sua
        localização, não acessa contatos, câmera, arquivos do celular fora do arquivo de
        extrato que você escolhe importar, e não usa nenhuma ferramenta de rastreamento,
        analytics ou publicidade de terceiros. Isso pode ser conferido diretamente no
        código fonte do app: não há nenhuma permissão sensível declarada, nem dependência
        de bibliotecas de anúncio, analytics ou rastreamento.


        3. BASE LEGAL

        O tratamento dos seus dados pessoais é feito com base no seu CONSENTIMENTO (Art.
        7º, inciso I, da LGPD), dado de forma livre, informada e inequívoca ao marcar a
        caixa de aceite deste termo na criação da sua conta.


        4. ONDE OS DADOS FICAM ARMAZENADOS E COMO SÃO PROTEGIDOS

        Todos os seus dados são armazenados na infraestrutura do Firebase (Google Cloud):
        Firebase Authentication para os dados de login, e Cloud Firestore (banco de dados)
        para todo o restante (perfil, bancos, cartões, simulações, extratos importados,
        listas e o próprio registro deste aceite). O Firebase/Google Cloud é o ÚNICO
        terceiro que processa seus dados, atuando como operador dos dados (na definição da
        LGPD), seguindo as instruções técnicas do Dinlux, e sujeito aos próprios padrões de
        segurança e certificações internacionais do Google Cloud.

        Proteções técnicas em vigor: toda comunicação entre o app e o Firebase é
        criptografada em trânsito (HTTPS/TLS), então ninguém consegue interceptar e ler
        esses dados no meio do caminho. Os dados armazenados (em repouso) também são
        criptografados pela própria infraestrutura do Google Cloud. O acesso aos seus dados
        no Firestore é restrito por regras de segurança que só permitem que uma conta
        autenticada leia ou modifique os PRÓPRIOS dados, nunca os de outro usuário. Sua
        senha nunca é armazenada em texto puro em lugar nenhum (ver seção 2.1). E nenhum
        dado seu é vendido, alugado ou compartilhado com anunciantes, corretores de dados
        ou qualquer outro terceiro além do próprio Firebase/Google Cloud citado acima, que
        atua só como infraestrutura de armazenamento.


        5. REGISTRO DO SEU ACEITE (EXIGÊNCIA DA LGPD)

        A LGPD exige não apenas que o consentimento seja obtido, mas que a empresa ou
        desenvolvedor consiga comprovar esse consentimento se precisar. Por isso, quando
        você aceita este termo na criação da conta, o Dinlux grava, junto com o seu
        perfil: que você aceitou (sim ou não), a data e hora exatas do aceite, e a versão
        exata deste texto que você aceitou. Esse registro pode ser consultado a qualquer
        momento pela opção "Políticas de Privacidade" no menu lateral do app, que mostra
        a data em que você aceitou junto com o texto completo.


        6. SEUS DIREITOS COMO TITULAR DOS DADOS (ART. 18 DA LGPD)

        Você tem direito a, a qualquer momento: confirmar a existência de tratamento e
        ACESSAR seus dados, já que tudo que o Dinlux guarda sobre você fica visível dentro
        das próprias telas do app (Finanças, Simulações, Avisos, Listas, Extratos);
        CORRIGIR dados incompletos, inexatos ou desatualizados, já que nome, e-mail e senha
        são editáveis pelo painel de perfil, e os dados financeiros são editáveis nas telas
        normais de cada módulo; ELIMINAR seus dados através da opção "Excluir Conta" no
        painel lateral (essa ação é permanente, não pode ser desfeita, e apaga TUDO: todos
        os bancos, cartões, simulações, lançamentos, extratos importados, listas, o
        documento do seu perfil e o registro deste termo aceito; nada fica guardado depois
        da exclusão, e a conta de login do Firebase Authentication também é excluída, como
        último passo); PORTABILIDADE dos seus dados a outro fornecedor, quando aplicável;
        saber com quem seus dados são compartilhados (resposta: só com o Firebase/Google
        Cloud, como infraestrutura de armazenamento, ver seção 4); e REVOGAR o
        consentimento a qualquer momento, o que na prática significa excluir a conta, já
        que essa é a finalidade central do app.


        7. RETENÇÃO DE DADOS

        Seus dados ficam armazenados enquanto sua conta existir, ou até você solicitar a
        exclusão. Não há compartilhamento com terceiros para fins de retenção prolongada,
        backup externo ou qualquer finalidade além do funcionamento do próprio app.


        8. MENORES DE IDADE

        O Dinlux não é direcionado a menores de 18 anos e não coleta intencionalmente dados
        de menores. Se você tem menos de 18 anos, use o app apenas com a supervisão e o
        consentimento de um responsável legal.


        9. ALTERAÇÕES NESTE TERMO

        Se este termo for atualizado de forma relevante no futuro, a versão numerada no
        topo deste texto muda, e pode ser solicitado um novo aceite explícito. O histórico
        de qual versão você aceitou e quando fica sempre preservado.


        10. RESUMO EM POUCAS PALAVRAS

        Seus dados financeiros (saldo, cartões, extratos) são só seus, ficam guardados de
        forma protegida no Firebase/Google Cloud, nunca são vendidos ou compartilhados com
        anunciantes, e servem só para o próprio app calcular suas simulações e mostrar seus
        gráficos. Você pode ver, corrigir ou apagar tudo a qualquer momento, e apagar sua
        conta apaga literalmente tudo, sem deixar rastro, inclusive este aceite.
    """.trimIndent()
}
