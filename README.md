# OP Drop Manhunt

Mod de Manhunt para **Minecraft 26.2 (Fabric)**.

Os **Runners** precisam derrotar o Ender Dragon. Os **Hunters** precisam matar os Runners.
A cada momento existe **um bloco ou mob secreto que é o "OP"**: quando um Runner quebra
esse bloco ou mata esse mob, ele ganha um pacote de itens muito fortes. Os Hunters sabem
qual é o alvo; os Runners só veem underscores, e uma letra é revelada por minuto.

Com o passar do tempo os dois lados ficam mais fortes: os Hunters ganham armadura a cada
10 minutos, e os OP drops sobem de tier junto.

---

## Requisitos

| Item | Versão |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.2 |
| Java | 25 |
| Fabric Loom | 1.17-SNAPSHOT |

Toda a lógica roda no **servidor**. Funciona tanto em mundo singleplayer aberto para
amigos (servidor integrado, tipo Essential) quanto em servidor dedicado. Não usa nada
client-only: só itens e componentes vanilla, títulos, action bar, chat e sons.

## Como compilar

```bash
./gradlew build
```

O `.jar` final sai em `build/libs/opdropmanhunt-1.0.0.jar`. Instale ele junto com a
**Fabric API** na pasta `mods/`.

Como o 26.2 não é mais ofuscado, não existe etapa de `remapJar`: o jar de `build/libs`
já é o artefato instalável.

Para testar no jogo:

```bash
./gradlew runClient
```

---

## Comandos

Todos ficam sob `/manhunt` e têm autocomplete.

### Times (qualquer jogador)

| Comando | O que faz |
|---|---|
| `/manhunt join runner` | Você entra no time dos Runners |
| `/manhunt join hunter` | Você entra no time dos Hunters |
| `/manhunt leave` | Você sai do seu time |
| `/manhunt teams` | Lista quem está em cada time |

Entrar e sair de time fica **bloqueado durante uma partida em andamento**.

### Partida (op)

| Comando | O que faz |
|---|---|
| `/manhunt team <jogador> <runner\|hunter>` | Define o time de outro jogador |
| `/manhunt start` | Começa a partida (exige pelo menos 1 runner e 1 hunter) |
| `/manhunt stop` | Encerra a partida sem vencedor |

### Teste / gravação (op)

| Comando | O que faz |
|---|---|
| `/manhunt skip` | Sorteia um novo alvo OP na hora |
| `/manhunt settier <1-6>` | Força o tier de drop e a armadura correspondente dos Hunters |
| `/manhunt reveal` | Revela mais uma letra imediatamente |
| `/manhunt status` | Mostra tempo, tier, alvo atual e letras reveladas |

Em mundo singleplayer, o host precisa estar com **cheats ativados** para usar os
comandos de op.

---

## Como funciona

### `/manhunt start`

- Todos os jogadores dos dois times vão para sobrevivência, com vida e fome cheias,
  inventário/efeitos/XP limpos.
- O horário do mundo vira dia.
- Título **"Game Start"** amarelo no centro da tela, com som.
- Hunters recebem a bússola e começam **sem armadura**.
- Sorteia o primeiro alvo OP e liga os timers (upgrade e revelação de letra).

### Bússola "Runner Tracker" (Hunters)

Aponta para o Runner vivo mais próximo na mesma dimensão, atualizando a cada 10 ticks.
Se nenhum Runner estiver na dimensão do Hunter, aponta para a última posição conhecida
de um Runner ali (normalmente o portal por onde ele saiu).

Ela **não pode ser perdida**: não dropa com Q, não dropa ao arrastar para fora do
inventário, não dropa na morte, e volta sozinha no respawn. A cada segundo o mod confere
se todo Hunter tem a dele, remove as que estiverem com quem não é Hunter e limpa as que
sobraram no chão ou em baús por perto.

### Upgrade dos Hunters

| Tempo | Armadura | Tier de drop |
|---|---|---|
| 0 min | nenhuma | 1 |
| 10 min | couro | 2 |
| 20 min | cobre | 3 |
| 30 min | ferro | 4 |
| 40 min | diamante | 5 |
| 50 min | netherite | 6 |
| 60 min | netherite encantada | 6 |

A armadura encantada vem com Protection IV + Unbreaking III em todas as peças e
Feather Falling IV nas botas. A cada upgrade sai **"Hunters have been UPGRADED"**
(vermelho, negrito) no chat, com som. Quando o tier de drop sobe junto, sai também
**"OP drops have been UPGRADED"** (verde, negrito) — por isso o upgrade de 60 min não
manda essa segunda mensagem, já que o tier já estava em 6.

A armadura é equipada direto nos slots, substituindo a anterior (a velha some, não
dropa), não cai na morte e volta no respawn.

### O alvo OP

A action bar verde mostra:

- Hunters e espectadores: `Current OP Drop: Iron Ore`
- Runners: `Current OP Drop: ____ ___` (os espaços entre palavras são mantidos)

A cada minuto uma letra aleatória ainda escondida é revelada para os Runners
(`C__y`). O contador reinicia quando o alvo muda.

Quando um **Runner** quebra o bloco (em modo de jogo normal) ou mata o mob:

1. Os itens OP caem no local, com um estouro para cima, partículas e som.
2. Sai no chat **"\<Runner\> found the OP drop: \<Nome\>!"** em verde.
3. Um novo alvo é sorteado na hora, escondido de novo.

Para mobs vale o **crédito de kill**, então conta flecha, tridente e dano indireto.
Se um **Hunter** quebrar o bloco ou matar o mob, não acontece nada: drop normal e o alvo
não muda. O drop vanilla continua acontecendo normalmente para todo mundo.

### Sorteio do novo alvo

- Só alvos da **dimensão em que o Runner está** (no início da partida, Overworld).
- Filtro de dificuldade pelo tier: tiers 1-2 → fácil, tiers 3-4 → fácil/médio,
  tiers 5-6 → médio/difícil. Os alvos do End valem em qualquer tier.
- Não repete nenhum dos **últimos 5** alvos.
- Se o filtro ficar vazio, a dificuldade é relaxada.

### Fim de jogo

- **Hunter morre:** respawn normal, com bússola e armadura do nível atual de volta.
- **Runner morre:** vira espectador.
- **Todos os Runners mortos:** título **"Hunters Wins!!"** em vermelho, com som.
- **Ender Dragon morre** (com pelo menos um Runner vivo): título **"Runners Wins!!"**
  em verde, com som e fogos perto dos Runners.

No fim da partida os timers param, a action bar para de ser enviada e os comandos de
time voltam a funcionar.

---

## Configuração

O arquivo `config/opdropmanhunt.json` é criado com os padrões na primeira execução:

```json
{
  "upgradeIntervalSeconds": 600,
  "letterRevealIntervalSeconds": 60,
  "entriesPerDrop": 3,
  "upToEnchantChance": 0.7
}
```

| Campo | O que é |
|---|---|
| `upgradeIntervalSeconds` | Intervalo entre upgrades de armadura / tier |
| `letterRevealIntervalSeconds` | Intervalo entre revelações de letra |
| `entriesPerDrop` | Quantas entradas diferentes cada drop sorteia |
| `upToEnchantChance` | Chance de um encantamento "até nível X" aparecer |

O arquivo é relido no `/manhunt start`, então dá para editar entre partidas sem
reiniciar o servidor.

## Editando os drops

Tudo que cai está em uma classe só:

```
src/main/java/com/opatosan/opdropmanhunt/opdrop/OpLootTables.java
```

Cada tier é uma lista de entradas com peso. Equipamento tem peso maior; comida e
minério, menor. Os atalhos para montar entradas são:

| Atalho | Para que serve |
|---|---|
| `stack(peso, item, min, max)` | Um item, quantidade em faixa |
| `gear(peso, item, encantamentos...)` | Equipamento com encantamentos |
| `armor(peso, set, encantamentos...)` | Uma peça aleatória do set |
| `food(peso, min, max)` | Um tipo aleatório de comida cozida |
| `combo(peso, ctx -> List.of(...))` | Uma entrada que dropa mais de um item |

E os encantamentos:

- `upTo(Enchantments.SHARPNESS, 3)` — 70% de chance de vir, com nível de 1 a 3.
- `fixed(Enchantments.DENSITY, 5)` — sempre vem, exatamente nesse nível.

Níveis acima do máximo do vanilla (Protection V) e combinações incompatíveis
(Density + Breach na mesma mace) são aplicados mesmo assim, porque o mod escreve direto
no componente de encantamentos do item.

Os alvos OP ficam em `opdrop/OpTargets.java` — é só adicionar uma linha para incluir um
bloco ou mob novo.

---

## Checklist de teste

Com `./gradlew runClient`, em um mundo com cheats:

- [ ] `join` / `leave` / `team` / `teams` funcionam e têm autocomplete
- [ ] `start` mostra "Game Start" amarelo e é bloqueado sem runner ou sem hunter
- [ ] Bússola aponta pro runner, não dropa com Q, não dropa na morte, volta no respawn
- [ ] `settier` aplica a armadura e as mensagens certas
- [ ] Action bar verde: hunter vê o nome, runner vê underscores, letra revelada por minuto
- [ ] Runner quebrando/matando o alvo dropa itens do tier certo e troca o alvo
- [ ] Hunter quebrando/matando o alvo não dropa nada OP e não troca o alvo
- [ ] Density V + Breach IV na mace e Protection V na netherite aparecem no item
- [ ] "Runners Wins!!" verde ao matar o dragão; "Hunters Wins!!" vermelho quando os runners morrem
- [ ] `stop` encerra tudo limpo

Dica para testar rápido: baixe `upgradeIntervalSeconds` para `30` e
`letterRevealIntervalSeconds` para `5` no config, ou use `/manhunt settier`,
`/manhunt reveal` e `/manhunt skip`.

---

## Estado do build

`./gradlew build` **compila limpo** contra o Minecraft 26.2, sem warnings, e gera
`build/libs/opdropmanhunt-1.0.0.jar`.

O que ainda **não** foi feito: o checklist de teste acima, jogando. Ele depende de
entrar no mundo com mais de um jogador, então fica com você.

### Mudanças de API do 26.2 que pegaram o código

Vale anotar, porque quebram qualquer coisa escrita com a cabeça na 1.21:

| Antes | No 26.2 |
|---|---|
| `ResourceLocation` | `net.minecraft.resources.Identifier` |
| `ResourceKey.location()` | `ResourceKey.identifier()` |
| `EntityType.COW` | `EntityTypes.COW` (as constantes saíram de `EntityType`) |
| `Level.setDayTime(long)` | sistema de clocks: `server.clockManager().setTotalTicks(clock, ticks)` |
| `PlayerTeam.setColor(ChatFormatting)` | `setColor(Optional<TeamColor>)` |
| `source.hasPermission(2)` | `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` |

O horário do mundo foi o mais invasivo: no 26.2 cada tipo de dimensão tem um clock
próprio, e o `/manhunt start` pega o clock padrão da dimensão
(`level.dimensionTypeRegistration().value().defaultClock()`) e adianta ele, que é
exatamente o caminho que o `/time set` usa.

### Verificações feitas

- IDs de item, bloco, entidade, encantamento, poção e componente conferidos contra o
  registro gerado do 26.2 (`stone_spear` e `copper_spear` existem; "Eye of Ender" é
  `ender_eye`, não `eye_of_ender`).
- Alvo do mixin conferido no jar real: `ServerPlayer.drop(ItemStack, boolean, boolean)`
  existe, e `Inventory.dropAll()` passa por ele — que é o que garante que a bússola e a
  armadura não caem na morte. Isso é uma string no `@Inject`, então o compilador não
  checa: se um dia parar de bater, o mod quebra ao carregar, não ao compilar.
