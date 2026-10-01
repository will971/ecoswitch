# EcoSwitch — Documentation Fonctionnelle

EcoSwitch est un comparateur d'aide à la décision écologique et financière conçu pour accompagner les particuliers et professionnels dans leur transition vers une mobilité plus propre. La plateforme permet de comparer les coûts d'utilisation d'un véhicule actuel (souvent thermique) avec des véhicules cibles (électriques, hybrides ou thermiques récents) afin d'identifier le seuil de rentabilité financière et le gain environnemental.

---

## 1. Périmètre Fonctionnel

La plateforme s'articule autour de quatre grands cas d'usage :

1. **Simulateur Direct** : Saisie libre de deux véhicules (actuel et cible) pour comparer instantanément leurs coûts de fonctionnement, leurs émissions de CO2, et intégrer des aides de l'État.
2. **Comparateur du Catalogue** : Comparaison d'un véhicule avec plusieurs alternatives issues du catalogue de référence (issu des données de l'ADEME).
3. **Gestion du Garage Virtuel (Profils)** : Enregistrement de ses propres véhicules pour des simulations ultérieures.
4. **Gestion du catalogue** : création et mise à jour des marques, modèles, motorisations, finitions et tarifs, réservée aux comptes ADMIN.

---

## 2. Indicateurs Clés & Formules de Calcul

Les calculs financiers et écologiques sont au cœur de la valeur d'EcoSwitch. Ils sont implémentés dans [cost-calculation.service.ts](../ecoswitch-api/src/comparison/cost-calculation.service.ts), portage exact de l'ancienne implémentation Java (vérifié sur 4 422 vecteurs de référence).

### A. Coût Total de Possession (TCO annuel)
Le coût annuel d'utilisation d'un véhicule est calculé comme suit :

$$\text{Coût Annuel} = \text{Coût Carburant Annuel} + \text{Coût Entretien}$$

Où le coût de carburant annuel est défini par :

$$\text{Coût Carburant Annuel} = \left( \frac{\text{Kilométrage Annuel}}{100} \right) \times \text{Consommation} \times \text{Prix du Carburant}$$

*Note : La consommation s'exprime en L/100km pour les thermiques/hybrides et en kWh/100km pour les électriques.*

### B. Pondération de la Recharge Électrique
Pour les véhicules électriques, le coût de l'électricité dépend du lieu de charge (domicile vs. borne publique rapide). L'utilisateur peut spécifier un ratio de charge à domicile ($R_{\text{domicile}} \in [0, 1]$). Le prix pondéré de l'électricité est alors calculé par :

$$\text{Prix Électricité Pondéré} = (R_{\text{domicile}} \times P_{\text{domicile}}) + ((1 - R_{\text{domicile}}) \times P_{\text{publique}})$$

*   $P_{\text{domicile}}$ est le tarif de l'électricité résidentiel saisi par l'utilisateur (ex: 0.25 €/kWh).
*   $P_{\text{publique}}$ est fixé au tarif moyen d'une borne publique rapide : **0.55 €/kWh**.

> Cette documentation annonçait auparavant 0,65 €/kWh. Le code a toujours utilisé 0,55 €/kWh, et c'est cette valeur qui fait foi.

### C. Seuil de Rentabilité (Break-Even)
Le seuil de rentabilité correspond au nombre d'années nécessaires pour que le cumul des économies d'utilisation compense le coût de l'investissement initial requis pour changer de véhicule.

$$\text{Investissement de Transition} = \text{Prix Achat Cible} - \text{Valeur de Revente Actuelle} - \text{Aides de l'État}$$

Le delta de coût au cours de l'année $Y$ est calculé par :

$$\text{Delta Coût}(Y) = (\text{Investissement de Transition} + \text{Coût Réparation Immédiat}) + (C_{\text{annuel, cible}} \times Y) - (C_{\text{annuel, actuel}} \times Y)$$

Le **seuil de rentabilité (en années)** est le premier entier $Y \ge 1$ pour lequel $\text{Delta Coût}(Y) \le 0$. Si ce seuil dépasse l'horizon maximal d'analyse (fixé à 15 ans), la transition est considérée comme non rentable financièrement sur cette période.

### D. Émissions de CO₂
EcoSwitch évalue l'impact écologique des véhicules en calculant leurs émissions de gaz à effet de serre en phase d'usage (du réservoir à la roue).

| Type d'Énergie (FuelType) | Facteur d'Émissions CO₂ (g/km pour 1 L/100km ou 1 kWh/100km) |
| :--- | :--- |
| **PETROL** (Essence) | Consommation $\times$ 23.0 |
| **DIESEL** (Gazole) | Consommation $\times$ 26.4 |
| **HYBRID** (Hybride) | Consommation $\times$ 20.0 |
| **PLUGIN_HYBRID** (Hybride rechargeable) | Consommation $\times$ 9.5 |
| **ELECTRIC** (Électricité) | Consommation $\times$ 0.5 (basé sur le mix électrique français bas carbone) |

Les émissions annuelles globales sont calculées par :

$$\text{Émissions Annuelles (kg CO₂)} = \frac{\text{Émissions (g/km)} \times \text{Kilométrage Annuel}}{1000}$$

---

## 3. Aides et Subventions Gouvernementales

L'application intègre les barèmes fiscaux français pour calculer automatiquement les subventions déductibles de l'achat.

### A. Bonus Écologique
Le bonus écologique s'applique à l'acquisition d'un véhicule propre (type `ELECTRIC`).
*   **Plafond du véhicule** : Le prix d'achat du véhicule cible doit être **inférieur ou égal à 47 000 €**.
*   **Barème selon les revenus** (Revenu Fiscal de Référence par part - RFR) :
    *   Si $\text{RFR} \le 15\ 400\ €$ : **7 000 €**
    *   Si $\text{RFR} > 15\ 400\ €$ ou non renseigné : **4 000 €**

### B. Prime à la Conversion
La prime à la conversion est octroyée lors de la mise au rebut (scrap) d'un vieux véhicule thermique (type `PETROL` ou `DIESEL`) au profit d'un véhicule propre (`ELECTRIC`, `HYBRID` ou `PLUGIN_HYBRID`).
*   **Barème selon les revenus** :
    *   Si $\text{RFR} \le 15\ 400\ €$ : **3 000 €**
    *   Si $\text{RFR} > 15\ 400\ €$ ou non renseigné : **1 500 €**

---

## 4. Modalités de Financement : Mode Leasing (LOA / LLD)

Si l'utilisateur sélectionne l'option de financement en **Leasing**, EcoSwitch évalue le coût d'utilisation mensuel global.
Si aucun loyer personnalisé n'est fourni, l'application estime une mensualité réaliste sur 48 mois via des hypothèses du marché de l'automobile :
*   **Premier loyer majoré (apport)** : 10 % du prix d'achat.
*   **Valeur résiduelle en fin de contrat** : 45 % du prix d'achat.
*   **Taux d'intérêt annuel nominal** : 3.9 % (Money Factor d'environ 0.0016).
*   **Durée du contrat** : 48 mois.

Le calcul de la mensualité est décomposé en amortissement de la part consommée et intérêt sur la valeur résiduelle bloquée :

$$\text{Mensualité} = \left( \frac{\text{Capital Financé} \times t_m}{1 - (1 + t_m)^{-48}} \right) + (\text{Valeur Résiduelle} \times t_m)$$

Où :
*   $\text{Capital Financé} = \text{Prix d'Achat} - \text{Apport} - \text{Valeur Résiduelle}$
*   $t_m = \frac{3.9\%}{12} = 0.00325$ (Taux mensuel)

---

## 5. Recherche par Plaque d'Immatriculation

Sur l'écran « carburant » du simulateur, l'utilisateur peut saisir sa plaque (format `AB-123-CD` ou `1234 AB 56`). Le champ est **facultatif** : en cas de succès, la marque, le modèle, le carburant, la consommation, l'entretien et la valeur de revente sont préremplis, puis l'utilisateur confirme sa consommation ; en cas d'échec, il poursuit simplement par le choix manuel du carburant.

1.  **Oscaro (en direct)** : l'API interroge `https://www.oscaro.com/catalog/vehicles/by_registration`.
2.  **Dictionnaire local de secours** : en cas d'erreur réseau, de blocage ou de plaque inconnue, l'API bascule sur [fallback-plates.json](../ecoswitch-api/src/immatriculation/fallback-plates.json).

Résultats mis en cache 24 h ; 20 recherches par minute et par IP au maximum.

> [!WARNING]
> Oscaro est interrogé sans convention, comme le ferait un navigateur. C'est fragile (un changement de leur site casse la fonction) et juridiquement discutable en usage commercial. L'API officielle du SIV, ou un fournisseur sous contrat, serait la cible à terme.

> [!TIP]
> **Plaques de test du dictionnaire local** (les plaques `AA-123-AA`… citées par une version précédente de cette page n'ont jamais existé) :
> *   `AB-123-CD` : Peugeot 208 II 1.2 PureTech 100 (Essence)
> *   `AA-987-BB` : Peugeot 3008 II 1.5 BlueHDi 130 (Diesel)
> *   `FR-199-XC` : Renault Clio V E-Tech 140 (Hybride)
> *   `GG-555-EL` : Tesla Model 3 Grande Autonomie (Électrique)
> *   `DK-120-FR` : Renault Zoe R135 (Électrique)
> *   `EZ-999-ZZ`, `DA-015-CM` : BMW 114i (Essence)
