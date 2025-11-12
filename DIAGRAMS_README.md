# Architecture Diagrams

Questo progetto include tre diagrammi PlantUML che illustrano l'architettura esagonale implementata:

## 📊 Diagrammi Disponibili

### 1. `ARCHITECTURE_DIAGRAM.puml` - Diagramma delle Classi
**Cosa mostra**: Tutte le classi del framework e del progetto di test con le loro relazioni
- **Framework Core** (blu): Classi del framework riutilizzabile
- **Test Project** (giallo): Implementazione specifica del progetto di test  
- **Protobuf Classes** (rosso): Classi generate automaticamente dai file `.proto`
- **Interfaces** (verde): Porte primarie e secondarie

### 2. `EXECUTION_FLOW_DIAGRAM.puml` - Flusso di Esecuzione
**Cosa mostra**: Come la stessa business logic viene eseguita attraverso protocolli diversi
- **REST Flow**: Richiesta HTTP → Use Case → Risposta HTTP
- **RabbitMQ Flow**: Messaggio → Use Case → Conferma
- **Direct Call Flow**: Chiamata diretta per testing

### 3. `PACKAGE_STRUCTURE_DIAGRAM.puml` - Struttura dei Package
**Cosa mostra**: Organizzazione dei package e dipendenze tra moduli
- **Framework packages**: Componenti riutilizzabili
- **Test project packages**: Implementazione specifica
- **Generated packages**: Classi Protobuf auto-generate

## 🔍 Come Visualizzare i Diagrammi

### Opzione 1: VS Code con PlantUML Extension
1. Installa l'estensione "PlantUML" in VS Code
2. Apri un file `.puml`
3. Premi `Alt+D` per vedere l'anteprima

### Opzione 2: Online PlantUML Server
1. Vai su http://www.plantuml.com/plantuml/uml/
2. Copia e incolla il contenuto di un file `.puml`
3. Clicca "Submit" per generare il diagramma

### Opzione 3: PlantUML CLI (se installato)
```bash
# Genera PNG da tutti i diagrammi
plantuml *.puml

# Genera SVG (vettoriale)
plantuml -tsvg *.puml
```

### Opzione 4: IntelliJ IDEA
1. Installa il plugin "PlantUML integration"
2. Apri un file `.puml`
3. Vedrai l'anteprima nel pannello laterale

## 🎯 Cosa Evidenziano i Diagrammi

### Separazione Framework vs Progetto
- **Framework** (blu): Componenti riutilizzabili, protocol-agnostic
- **Test Project** (giallo): Implementazione specifica con business logic

### Principi Architetturali
- **Inversione delle Dipendenze**: Use cases dipendono da interfacce, non da implementazioni
- **Single Responsibility**: Ogni classe ha una responsabilità specifica
- **Protocol Independence**: Business logic non conosce REST, RabbitMQ, etc.

### Flusso di Esecuzione
- **Stesso Use Case**: Eseguito identicamente via REST, RabbitMQ, o chiamata diretta
- **Validation**: Centralizzata nel framework
- **Error Handling**: Gestito uniformemente per tutti i protocolli

## 🔧 Personalizzazione

Per modificare i diagrammi:
1. Modifica i file `.puml`
2. Usa i colori definiti:
   - `FRAMEWORK_COLOR #E8F4FD` (blu chiaro)
   - `TEST_PROJECT_COLOR #FFF2CC` (giallo chiaro)
   - `PROTOBUF_COLOR #F8CECC` (rosso chiaro)
   - `INTERFACE_COLOR #D5E8D4` (verde chiaro)

## 📚 Documentazione Correlata

- `HEXAGONAL_ARCHITECTURE.md` - Spiegazione dettagliata dell'architettura
- `DEMO_EXAMPLE.md` - Esempio pratico di utilizzo
- `IMPLEMENTATION_SUMMARY.md` - Riassunto dell'implementazione

## 🎨 Esempio di Output

I diagrammi mostrano chiaramente:
- Come il framework separa business logic dai protocolli
- Come lo stesso use case può essere esposto via REST e RabbitMQ
- Come testare la business logic in isolamento
- Come aggiungere nuovi protocolli senza modificare il codice esistente

Questi diagrammi sono essenziali per comprendere i benefici dell'architettura esagonale implementata nel progetto.