# TownyChestBank

Plugin Spigot/Paper qui remplace la banque virtuelle de Towny par un **coffre physique** placé
dans le monde. Les joueurs déposent et retirent de l'argent en manipulant de vrais objets, selon
les permissions de plot Towny configurées par les admins.

---

## Fonctionnement général

À la création d'une ville, un coffre est automatiquement placé à la surface du homeblock. Ce
coffre ouvre une interface personnalisée à la place de l'interface vanilla. Les commandes Towny
classiques de dépôt et de retrait sont bloquées et redirigées vers le coffre.

### Interface du coffre

L'inventaire custom (27 cases) est divisé en trois rangées.

| Rangée      | Contenu                                                                                    |
| ----------- | ------------------------------------------------------------------------------------------ |
| 1 — Info    | Solde actuel de la banque au centre, séparateurs de verre                                  |
| 2 — Retrait | Un slot par type de monnaie configuré ; clic gauche = tout retirer, clic droit = retirer 1 |
| 3 — Dépôt   | Zone libre : poser les items ici, ils sont convertis à la fermeture du coffre              |

**Dépôt rapide :** shift-clic sur une monnaie depuis l'inventaire du joueur → conversion immédiate
sans passer par la zone de staging.

---

## Installation

1. Placer `TownyChestBank-1.0.0.jar` dans le dossier `plugins/`.
2. S'assurer que **Towny Advanced ≥ 0.101.x** est installé.
3. Redémarrer le serveur — `config.yml` est généré automatiquement.

### Compatibilité

| Composant      | Version requise |
| -------------- | --------------- |
| Minecraft      | 1.20.1          |
| Towny Advanced | 0.101.x         |
| Java           | 11 ou supérieur |

---

## Configuration

```yaml
# config.yml — monnaies et valeurs en pièces d'or (po)
currency:
  GOLD_NUGGET: 1.0
  GOLD_INGOT: 9.0
  GOLD_BLOCK: 81.0
  EMERALD: 10.0
  DIAMOND: 50.0
```

Tout matériau Minecraft valide peut être utilisé comme monnaie. L'ordre des entrées détermine
l'ordre des slots de retrait dans l'interface.

---

## Commandes

| Commande             | Description                                               | Permission requise          |
| -------------------- | --------------------------------------------------------- | --------------------------- |
| `/tchestbank set`    | Enregistre le coffre visé comme coffre-banque de la ville | `townychestbank.set`        |
| `/tchestbank remove` | Retire l'enregistrement du coffre-banque                  | `townychestbank.set`        |
| `/tchestbank info`   | Affiche la position et le solde du coffre-banque          | Aucune (membre de la ville) |

Le coffre désigné avec `/tchestbank set` doit être situé dans le **homeblock** ou un **plot de
type Bank** de la ville. Seuls le maire et les assistants peuvent exécuter `set` et `remove`.

---

## Permissions

| Nœud                   | Description                                  | Défaut |
| ---------------------- | -------------------------------------------- | ------ |
| `townychestbank.use`   | Accès aux commandes de base                  | Tous   |
| `townychestbank.set`   | Définir ou déplacer le coffre-banque         | Op     |
| `townychestbank.admin` | Accès admin complet, bypass des checks Towny | Op     |

### Permissions de plot Towny

L'ouverture, le dépôt et le retrait vérifient la permission **SWITCH** du plot contenant le coffre
via `PlayerCacheUtil`. Le comportement est donc entièrement piloté par les flags `/t set perm` du
plot homeblock ou du plot Bank.

```
# Exemple : autoriser uniquement les résidents à utiliser le coffre
/t set perm res switch on
/t set perm out switch off
```

La permission `townychestbank.admin` contourne tous les checks Towny.

---

## Commandes Towny bloquées

Les commandes suivantes sont interceptées et redirigent le joueur vers le coffre-banque.

- `/t deposit` et `/t withdraw`
- `/town deposit` et `/town withdraw`
- `/ta town <ville> deposit` et `/ta town <ville> withdraw`
- `/townyadmin town <ville> deposit` et `/townyadmin town <ville> withdraw`

---

## Protection du coffre

- Indestructible pour les non-maires et les non-admins.
- Résistant aux explosions (TNT, creeper, etc.).
- Le maire peut casser le coffre ; l'enregistrement est alors automatiquement retiré.

---

## Données persistantes

Les associations `ville → coffre` sont stockées dans `plugins/TownyChestBank/data.yml`. Le solde
reste géré par Towny (via l'API `Account`).

---

## Compilation

```bash
mvn package
# Produit : target/TownyChestBank-1.0.0.jar
```
