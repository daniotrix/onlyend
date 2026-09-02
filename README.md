# Only End

Mod de Fabric para Minecraft 26.2.

**Lee en:** [Español](#español) | [English](#english)

---

## Español

### Qué hace

Empezás en el End y nunca salís de ahí — ni ves el Overworld ni el Nether en ningún momento, ni al conectarte por primera vez ni al reaparecer. Todo lo que en vanilla depende de otras dimensiones (madera y piedra, blazes y varas de blaze, el Stronghold, el portal de salida, el Dragón) está reubicado o adaptado para poder conseguirse sin salir de esa única dimensión. El objetivo final sigue siendo el mismo que en vanilla: encontrar el Stronghold, entrar al End, y vencer al Ender Dragon.

### Cómo funciona

**Aparición y reaparición.** Al conectarte o reaparecer siempre terminás en el End — nunca llegás a ver un solo chunk del Overworld, ni por un instante. En vez de la plataforma clásica junto al Dragón, aparecés cerca de la End City más próxima al punto de entrada clásico. Si no hay ninguna cerca, usa esa plataforma de siempre como respaldo. Nunca podés viajar a otra dimensión: cualquier intento (portal del Nether, portal de salida del End) se cancela. Si caés al vacío, te rescata antes de que la caída termine matándote.

**Madera y piedra.** Partes de las islas del End se convierten en terreno "normal" — pasto, tierra, piedra, distintos tipos de árboles, animales de granja — así se puede conseguir madera y piedra sin salir de la dimensión.

**Endermen.** Es el único monstruo natural del End, así que sin ajustar se vuelve abrumador: los grupos de aparición son más chicos que en vanilla y, además, la mayoría de los intentos de aparición se descartan — quedan como una amenaza ocasional en vez de una plaga constante.

**La Fortaleza del Nether, reubicada.** Se genera dentro del End (en vez del Nether) usando el mismo generador de piezas original, así que sigue teniendo la misma forma — solo que los ladrillos del Nether se reemplazan por sus equivalentes del End (Purpur, End Stone, End Rod) apenas termina de generarse. El Blaze pasa a ser el mob dominante de la fortaleza (50% de probabilidad en cada intento de aparición, en grupos de 4 a 8), así que conseguir varas de Blaze sigue siendo viable sin pisar el Nether.

**Encontrar el Stronghold.** El Stronghold no existe en el mundo hasta que lanzás tu primer Ojo de Ender estando en el End — ese lanzamiento elige una dirección al azar y busca, a una distancia de 2000 a 3000 bloques, el primer punto que caiga en un bioma válido, y fuerza que el Stronghold se genere ahí mismo. A partir de ese momento, cualquier Ojo de Ender que tires apunta hacia ese Stronghold ya elegido — igual que en vanilla, si el objetivo está lejos el Ojo no llega en un solo lanzamiento y hay que ir hasta donde cae y volver a tirar. Por la forma en que está implementado, `/locate structure` nunca lo va a encontrar — no es un bug, es una limitación conocida (más detalles abajo). El Stronghold en sí también está reskinizado con los mismos bloques del End que la fortaleza.

**Los "gateways de vuelta" de las End Cities.** Vanilla coloca, con baja probabilidad, un End Gateway suelto en algunas islas exteriores — un atajo pensado para volver cerca del punto de entrada de un salto. Acá ese mismo punto de generación (mismo lugar, misma rareza, misma jaula de bedrock alrededor) coloca lava en el centro en vez de un portal funcional, así que ya no sirve como atajo para saltearse la exploración y llegar antes de tiempo a la isla del Dragón.

**Del Stronghold a la isla del Dragón, y de vuelta.** Cuando encontrás el Stronghold y completás su portal clásico con Ojos de Ender, entrar te lleva directo a la isla del Dragón (con su plataforma de obsidiana, creada en el momento porque acá nadie entra por un portal real). Si el Dragón todavía no murió, cualquier portal que toques en el End solo puede ser ese portal del Stronghold. Una vez que matás al Dragón y aparece su portal de salida real, entrar ahí sí muestra los créditos de verdad — y al terminar, te regresa a tu punto de reaparición configurado (el mismo que usa el sistema de aparición), nunca al Overworld.

### Limitaciones conocidas

- `/locate structure onlyend:end_stronghold` nunca va a encontrar nada — el Stronghold usa un tipo de ubicación (`placement`) propio del mod, y el buscador nativo de `/locate` solo reconoce los tipos `minecraft:concentric_rings` y `minecraft:random_spread`. Para confirmar dónde se generó, revisá el log del server (línea `Only End: Stronghold forzado en el chunk [...]`).
- El Ojo de Ender no viaja toda la distancia de un solo lanzamiento cuando el objetivo está muy lejos (2000-3000 bloques) — como en vanilla, hay que ir hasta donde se hunde y volver a lanzar desde ahí, repitiendo hasta acercarse lo suficiente.

---

## English

### What it does

You start in the End and never leave it — you never see the Overworld or the Nether, not on first join and not on respawn. Everything that vanilla ties to another dimension (wood and stone, blazes and blaze rods, the Stronghold, the exit portal, the Dragon fight) has been relocated or adapted so it can be obtained without ever leaving that one dimension. The end goal is still the same as vanilla: find the Stronghold, step into the exit portal, and defeat the Ender Dragon.

### How it works

**Spawning and respawning.** Joining or respawning always puts you in the End — you never render a single Overworld chunk, not even for an instant. Instead of the classic platform next to the Dragon, you land near the End City closest to the vanilla entry point; if none is nearby, it falls back to that classic platform. You can never travel to another dimension — any attempt (Nether portal, End's own exit portal) is cancelled outright. Falling into the void rescues you before the fall would otherwise kill you.

**Wood and stone.** Parts of End islands get converted into "normal" terrain — grass, dirt, stone, a mix of tree types, farm animals — so you can gather wood and stone without leaving the dimension.

**Endermen.** They're the only natural monster in the End, so left alone they'd be overwhelming: spawn groups are smaller than vanilla, and on top of that most spawn attempts are discarded outright — they stay an occasional threat instead of a constant swarm.

**The Nether Fortress, relocated.** It generates inside the End (instead of the Nether) using the exact same original piece generator, so the layout is unchanged — only the Nether brick blocks get swapped for their End equivalents (Purpur, End Stone, End Rod) right after generation finishes. The Blaze becomes the fortress's dominant mob (50% chance on every spawn attempt, in groups of 4-8), so farming blaze rods stays viable without ever setting foot in the Nether.

**Finding the Stronghold.** The Stronghold doesn't exist in the world until you throw your first Ender Eye while in the End — that throw picks a random direction, scans outward between 2000 and 3000 blocks for the first point that lands in a valid biome, and forces the Stronghold to generate exactly there. From then on, every Ender Eye you throw points at that same Stronghold — just like vanilla, if the target is far away the Eye won't cover the whole distance in one throw, so you follow it and throw again from where it lands. Because of how it's implemented, `/locate structure` will never find it — that's a known limitation, not a bug (details below). The Stronghold itself gets the same End-block reskin as the fortress.

**End City "return gateways".** Vanilla rarely places a standalone End Gateway on some outer islands — a shortcut meant to send you back near the world's entry point in one jump. That same spawn point (same location, same rarity, same bedrock cage around it) now places lava at the center instead of a working gateway, so it can no longer be used to skip exploration and reach the Dragon island early.

**From the Stronghold to the Dragon island, and back.** Once you find the Stronghold and complete its classic portal with Ender Eyes, stepping through sends you straight to the Dragon island (with its obsidian platform, created on the spot since nobody ever enters through a real portal here). If the Dragon hasn't died yet, any portal you touch in the End can only be that Stronghold portal. Once you kill the Dragon and its real exit portal appears, stepping into *that* one shows the actual credits — and afterward you're returned to your configured respawn point (the same one the spawn system uses), never to the Overworld.

### Known limitations

- `/locate structure onlyend:end_stronghold` will never find anything — the Stronghold uses a custom `placement` type, and vanilla's `/locate` search only recognizes `minecraft:concentric_rings` and `minecraft:random_spread`. To confirm where it generated, check the server log for the line `Only End: Stronghold forzado en el chunk [...]`.
- The Ender Eye doesn't cover the whole distance in a single throw when the target is far away (2000-3000 blocks) — just like vanilla, you have to follow it to where it lands and throw again, repeating until you're close enough.
