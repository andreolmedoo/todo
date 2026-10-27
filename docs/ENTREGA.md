# App 02 — Aplicativo com Persistência Local (SQLite)

**Aplicativo:** TaskFlow — gerenciador de tarefas pessoais
**Disciplina:** Desenvolvimento para Dispositivos Móveis — Prof. Alessandro Brawerman

---

## 1. Integrantes do grupo

| Nome completo | GRR |
| --- | --- |
| `<Nome do integrante 1>` | `<GRRxxxxxxxx>` |
| `<Nome do integrante 2>` | `<GRRxxxxxxxx>` |
| `<Nome do integrante 3>` | `<GRRxxxxxxxx>` |
| `<Nome do integrante 4 — remover a linha se o grupo tiver 3>` | `<GRRxxxxxxxx>` |

## 2. Repositório

`<https://github.com/usuario/repositorio>`

O histórico de commits incrementais do desenvolvimento está disponível no repositório acima.

## 3. Tema e explicação do aplicativo

O TaskFlow é um gerenciador de tarefas pessoais que permite ao usuário cadastrar
compromissos do dia a dia com título, descrição, prazo e prioridade, acompanhar
quais já foram concluídos e quais estão pendentes ou atrasados, e manter tudo
salvo no próprio aparelho. Toda informação é gravada em um banco SQLite local,
de modo que os dados continuam disponíveis depois de fechar o aplicativo ou
reiniciar o dispositivo. A tela inicial concentra a lista de tarefas, o resumo de
progresso e os filtros de consulta; a segunda tela concentra o cadastro, a edição
e a exclusão de um registro.

## 4. Regras de negócio

| # | Regra | Onde está implementada |
| --- | --- | --- |
| 1 | **Situação da tarefa** — toda tarefa é pendente ou concluída, controlado pelo campo `completed`. | `TaskFormActivity` (CheckBox) e coluna `completed` |
| 2 | **Resumo de progresso** — a tela principal mostra "X de Y tarefas concluídas (Z%)", calculado a partir do total de registros e do total de concluídas. | `MainActivity.updateProgress()`, `TaskDAO.countAll()` / `countCompleted()` |
| 3 | **Tarefa atrasada** — uma tarefa com prazo definido, ainda não concluída e com prazo anterior à data de hoje é exibida como *Atrasada* (em vermelho). | `DateUtils.isOverdue()` e `TaskAdapter` |
| 4 | **Prioridade** — cada tarefa recebe prioridade Baixa, Média ou Alta, usada na ordenação da lista. | `Task.priority`, `TaskFormActivity` (RadioGroup) |

## 5. Forma de ordenação

A lista é sempre ordenada pelo mesmo critério, definido na constante `ORDER_BY`
do `TaskDAO`:

1. **Pendentes antes das concluídas** (`completed ASC`);
2. **Prioridade mais alta primeiro** (`priority DESC`: Alta → Média → Baixa);
3. **Prazo mais próximo primeiro**, com as tarefas sem prazo no fim do grupo;
4. **Título em ordem alfabética** como critério de desempate (`COLLATE NOCASE`).

O prazo é gravado no banco no formato `yyyy-MM-dd` justamente para que a ordenação
textual do SQLite coincida com a ordem cronológica. Na tela a data é sempre
exibida e digitada como `dd/mm/aaaa`.

## 6. Filtros de listagem disponíveis

São três formas de consultar os dados, combináveis entre si:

| Filtro | Como o usuário usa | Implementação |
| --- | --- | --- |
| **Listar todas** | Spinner "Filtro" na opção *Todas* (padrão) | `TaskDAO.list(filter = TaskFilter.ALL)` |
| **Buscar por item específico** | Campo de busca + botão *Buscar* (busca parcial por título, sem diferenciar maiúsculas/minúsculas) | `title LIKE ?` em `TaskDAO.list()` |
| **Filtrar por situação** | Spinner "Filtro" nas opções *Pendentes* ou *Concluídas* | `completed = 0` / `completed = 1` |

O botão *Limpar* zera o texto da busca e volta o filtro para *Todas*.

## 7. Estrutura do projeto no Android Studio

Pacote base: `com.example.todo`

```
app/src/main/java/com/example/todo/
├── model/
│   └── Task.kt                 Entidade da tarefa (id, título, descrição, prazo,
│                               situação e prioridade) + constantes de prioridade
├── db/
│   └── DBHelper.kt             SQLiteOpenHelper: cria a tabela "tasks" em onCreate
│                               e faz a migração de versão em onUpgrade
├── dao/
│   ├── TaskDAO.kt              Todo o SQL do app: insert, update, delete, getById,
│   │                           list (busca + filtro), countAll, countCompleted
│   └── TaskFilter.kt           Enum com os filtros de listagem (ALL/PENDING/COMPLETED)
├── adapter/
│   └── TaskAdapter.kt          ArrayAdapter do ListView, monta cada linha da lista
├── controller/
│   ├── MainActivity.kt         Tela principal (lista, filtros, progresso)
│   └── TaskFormActivity.kt     Tela de cadastro/edição/exclusão
└── util/
    └── DateUtils.kt            Conversão e validação de datas e regra de "atrasada"

app/src/main/res/layout/
├── activity_main.xml           Tela principal
├── activity_task_form.xml      Formulário
└── item_task.xml               Linha da lista

app/src/test/java/...           Teste unitário das regras de data (DateUtilsTest)
app/src/androidTest/java/...    Teste instrumentado do CRUD, filtros e ordenação (TaskDAOTest)
```

### Banco de dados

- Arquivo: `todo.db` — versão **2**
- Tabela `tasks`:

| Coluna | Tipo | Observação |
| --- | --- | --- |
| `id` | INTEGER | chave primária AUTOINCREMENT |
| `title` | TEXT | obrigatório (CHECK impede texto vazio) |
| `description` | TEXT | opcional, padrão `''` |
| `due_date` | TEXT | opcional, formato `yyyy-MM-dd` |
| `completed` | INTEGER | 0 = pendente, 1 = concluída |
| `priority` | INTEGER | 1 = Baixa, 2 = Média, 3 = Alta |

A versão 1 do banco não tinha a coluna `priority`; o `onUpgrade` faz o
`ALTER TABLE ... ADD COLUMN` sem apagar os dados já cadastrados.

### Configuração

- **API 29** (`minSdk = 29`), template *Empty Views Activity*, Kotlin.
- Interface construída com Views tradicionais (sem Jetpack Compose) e persistência
  com `SQLiteOpenHelper` (sem Room), conforme o conteúdo visto em aula.

## 8. Telas desenvolvidas

### Tela 1 — `MainActivity` (lista de tarefas)

- Título do aplicativo;
- Campo de busca por título, com botões *Buscar* e *Limpar*;
- Spinner de filtro: *Todas*, *Pendentes*, *Concluídas*;
- Resumo de progresso: "X de Y tarefas concluídas (Z%)";
- `ListView` com as tarefas: título, descrição, prioridade, prazo e situação
  (*Pendente* em azul, *Atrasada* em vermelho, *Concluída* em verde);
- Mensagem "Nenhuma tarefa encontrada" quando o filtro não retorna registros;
- Botão *Adicionar tarefa*.

![Tela principal](img/tela-principal.png)

### Tela 2 — `TaskFormActivity` (cadastro / edição / exclusão)

- Título que muda entre *Nova tarefa* e *Editar tarefa*;
- Campos: título, descrição, prazo (`dd/mm/aaaa`), prioridade (RadioGroup) e
  marcação de concluída (CheckBox);
- Botão *Salvar* (insere ou atualiza, conforme a origem);
- Botão *Excluir*, visível somente na edição de uma tarefa existente;
- Botão *Cancelar*, que volta sem gravar nada.

![Tela de cadastro e edição](img/tela-formulario.png)

## 9. Fluxo de navegação e ações

```
            MainActivity (lista)
                   │
     ┌─────────────┴──────────────┐
     │                            │
  "Adicionar tarefa"        toque em um item
     │                            │
     ▼                            ▼
TaskFormActivity            TaskFormActivity
(EXTRA_TASK_ID = 0)         (EXTRA_TASK_ID = id da tarefa)
     │                            │
  Salvar → INSERT          Salvar  → UPDATE
                           Excluir → DELETE
     │                            │
     └──────────► volta para a MainActivity ◄──────┘
                 (onResume recarrega a lista)
```

- A navegação entre as telas usa `Intent` com `putExtra(EXTRA_TASK_ID, id)`.
- Ao voltar do formulário, o `onResume()` da `MainActivity` relê o banco, de modo
  que inclusões, edições e exclusões aparecem imediatamente na lista e no resumo
  de progresso.
- O botão físico/gestual de voltar sai do formulário sem alterar nada.

## 10. Validação de dados

Todas as validações acontecem na `TaskFormActivity`, **antes** de chamar o DAO.
Em qualquer erro é exibido um `Toast` e o foco volta para o campo inválido:

| Situação | Mensagem |
| --- | --- |
| Título em branco | "Informe o título da tarefa" |
| Título com mais de 100 caracteres | "O título deve ter no máximo 100 caracteres" |
| Descrição com mais de 500 caracteres | "A descrição deve ter no máximo 500 caracteres" |
| Prazo mal formatado ou data inexistente (ex.: `32/10/2026`, `29/02/2025`) | "Prazo inválido. Use o formato dd/mm/aaaa" |

Como reforço, o banco também possui restrições `CHECK` para título não vazio,
`completed` em (0,1) e `priority` em (1,2,3).

## 11. Persistência real

Os dados são gravados em SQLite (`todo.db`) no armazenamento interno do
aplicativo. Nenhuma lista em memória é usada como fonte dos dados: a cada
abertura da tela principal e a cada troca de filtro a lista é relida do banco.
Fechar o aplicativo ou reiniciar o dispositivo não apaga os registros.

## 12. Testes realizados

- **Teste unitário** (`DateUtilsTest`): conversão e validação do prazo, incluindo
  ano bissexto e datas inexistentes; regra de tarefa atrasada.
- **Teste instrumentado** (`TaskDAOTest`): inserção, consulta por id, atualização,
  exclusão, filtros (busca por título, pendentes, concluídas), ordenação
  personalizada e contadores do resumo de progresso.
- **Teste manual** no emulador: cadastro, edição, exclusão, filtros, validações e
  verificação de que os dados permanecem após fechar e reabrir o aplicativo.

## 13. Referências

Material da disciplina (aulas 01 a 24) e documentação oficial do Android
(`SQLiteOpenHelper`, `ListView`, `ArrayAdapter`, `Spinner`).
