package towa;
// test
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Votre IA pour le jeu Towa.
 */
public class IATowa {
    
    /**
     * Objet random utilisé par tous le programme.
     */
    static Random random = new Random();
    
    /**
     * Hôte du grand ordonnateur.
     */
    String hote;

    /**
     * Port du grand ordonnateur.
     */
    int port;

    /**
     * Couleur de votre joueur (IA) : 'N'oir ou 'B'lanc.
     */
    final char couleur;

    /**
     * Interface pour le protocole du grand ordonnateur.
     */
    TcpGrandOrdonnateur grandOrdo;

    /**
     * Nombre maximal de tours de jeu.
     */
    static final int NB_TOURS_JEU_MAX = 40;
    
    /**
     * Nombre d'action selectionnées parmi les mieux évaluées et les pires.
     */
    static final int NB_ACTION_SELEC = 6;
    
    /**
     * Profondeur de l'abre des possibilités calculées (coups d'avances).
     */
    static final int PROFONDEUR = 4;

    /**
     * Constructeur.
     *
     * @param hote Hôte.
     * @param port Port.
     * @param uneCouleur couleur du joueur
     */
    public IATowa(String hote, int port, char uneCouleur) {
        this.hote = hote;
        this.port = port;
        this.grandOrdo = new TcpGrandOrdonnateur();
        this.couleur = uneCouleur;
    }

    /**
     * Connexion au Grand Ordonnateur.
     *
     * @throws IOException exception sur les entrées/sorties
     */
    void connexion() throws IOException {
        System.out.print(
                "Connexion au Grand Ordonnateur : " + hote + " " + port + "...");
        System.out.flush();
        grandOrdo.connexion(hote, port);
        System.out.println(" ok.");
        System.out.flush();
    }

    /**
     * Boucle de jeu : envoi des actions que vous souhaitez jouer, et réception
     * des actions de l'adversaire.
     *
     * @throws IOException exception sur les entrées/sorties
     */
    void toursDeJeu() throws IOException {
        // paramètres
        System.out.println("Je suis le joueur " + couleur + ".");
        // le plateau initial
        System.out.println("Réception du plateau initial...");
        Case[][] plateau = grandOrdo.recevoirPlateauInitial();
        System.out.println("Plateau reçu.");
        // compteur de tours de jeu (entre 1 et 40)
        int nbToursJeu = 1;
        // la couleur du joueur courant (change à chaque tour de jeu)
        char couleurTourDeJeu = Case.CAR_NOIR;
        // booléen pour détecter la fin du jeu
        boolean fin = false;
        while (!fin) {
            boolean disqualification = false;

            if (couleurTourDeJeu == couleur) {
                // à nous de jouer !
                jouer(plateau, nbToursJeu);
            } else {
                // à l'adversaire de jouer
                disqualification = adversaireJoue(plateau, couleurTourDeJeu);
            }
            if (nbToursJeu == NB_TOURS_JEU_MAX || disqualification) {
                // fini
                fin = true;
            } else {
                // au suivant
                nbToursJeu++;
                couleurTourDeJeu = suivant(couleurTourDeJeu);
            }
        }
    }

    /**
     * Fonction exécutée lorsque c'est à notre tour de jouer. Cette fonction
     * envoie donc l'action choisie au serveur.
     *
     * @param plateau le plateau de jeu
     * @param nbToursJeu numéro du tour de jeu
     */
    void jouer(Case[][] plateau, int nbToursJeu) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        System.out.println("debut de jouer : lancement le " + format.format(new Date()));
        String actionJouee = actionChoisie(plateau, nbToursJeu);
        if (actionJouee != null) {
            // jouer l'action
            System.out.println("On joue : " + actionJouee);
            grandOrdo.envoyerAction(actionJouee);
            mettreAJour(plateau, actionJouee, couleur);
        } else {
            // Problème : le serveur vous demande une action alors que vous n'en
            // trouvez plus...
            System.out.println("Aucun action trouvée : abandon...");
            grandOrdo.envoyerAction("ABANDON");
        }
        System.out.println("fin de jouer : fin le le " + format.format(new Date()));
    }


    /**
     * L'action choisie par notre IA.
     *
     * @param plateau le plateau de jeu.
     * @param nbToursJeu nombre de tours écoulé depuis le début de la partie.
     * @return l'action choisie, sous forme de chaîne.
     */
    public String actionChoisie(Case[][] plateau, int nbToursJeu) {
        
        JoueurTowa joueur = new JoueurTowa();
        
        String[] actionsPossibles = joueur.actionsPossibles(plateau, couleur, 8);
        
        if(nbToursJeu != 1)actionsPossibles = selectionnerActions(actionsPossibles, couleur);
        
        actionsPossibles = enleverVitaliteTableau(actionsPossibles);
        
        return trouveMeilleurAction(plateau, actionsPossibles, nbToursJeu);
    }

    /**
     * Permet de trouver la meilleure action.
     * 
     * @param plateau du jeu.
     * @param actionsPossibles ensembles des actions possibles à partir de ce plateau.
     * @return la meilleure action
     */
    public String trouveMeilleurAction(Case[][] plateau, String[] actionsPossibles, int nbToursJeu){
        
        if(nbToursJeu==1){
            
            for(String action:actionsPossibles){
                System.out.print(action + " ");
            }
            
            int randomNumber = random.nextInt(actionsPossibles.length);
            System.out.println("nombre aleartoire:" + randomNumber);
            
            return actionsPossibles[randomNumber];
        }
        
        String meilleur_action = null;
        int meilleur_score = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE; // -infinie
        int beta = Integer.MAX_VALUE; // +infini
        
        for(String action:actionsPossibles){
            
            Case[][] plateauAModifier = copierPlateau(plateau);
            mettreAJour(plateauAModifier,action,couleur);
            int score = minMax(plateauAModifier,true, 0, alpha,beta);
            
            if(score>meilleur_score){
                meilleur_action = action;
                meilleur_score = score;
            }
            
            if (meilleur_score <= score ? : meilleur_action = action)
        }
        return meilleur_action;
    }

    /**
     * Fonction recursive visant à determiner le meilleur score
     * @param plateau plateau du jeu
     * @param profondeurTotale profondeur de recherches
     * @param profondeurCourante profondeur actuelle
     * @return score de l'action
     */
    public int minMax(Case[][] plateau, boolean maximising, int profondeurCourante,int alpha ,int beta) {
        JoueurTowa joueur = new JoueurTowa();
        String[] actionsPossibles;
        if (profondeurCourante >= PROFONDEUR) {
            return evaluatePlateau(plateau);
        }
        if (maximising) {
            int score = Integer.MIN_VALUE;
            actionsPossibles = joueur.actionsPossibles(plateau, couleur, 8);
            actionsPossibles = selectionnerActions(actionsPossibles,couleur);
            actionsPossibles = enleverVitaliteTableau(actionsPossibles);
            for (String action : actionsPossibles) {
                Case[][] plateauCopie = copierPlateau(plateau);
                mettreAJour(plateauCopie, action, couleur);
                score = Math.max(score,minMax(plateauCopie, false, profondeurCourante + 1,alpha,beta));
                alpha = Math.max(alpha, score);
                if (score >= beta) {
                    break; // Élagage alpha
                }

            }
            return score;
        } else {
            int score = Integer.MAX_VALUE;
            actionsPossibles = joueur.actionsPossibles(plateau, Utils.inverseCouleurJoueur(couleur), 8);
            actionsPossibles = selectionnerActions(actionsPossibles,Utils.inverseCouleurJoueur(couleur));
            actionsPossibles = enleverVitaliteTableau(actionsPossibles);
            for (String action : actionsPossibles) {
                Case[][] plateauCopie = copierPlateau(plateau);
                mettreAJour(plateauCopie, action, Utils.inverseCouleurJoueur(couleur));
                score = Math.min(score,minMax(plateauCopie, true, profondeurCourante + 1,alpha,beta));
                beta = Math.min(beta, score);
                if (score <= alpha) {
                    break; // Élagage beta
                }
            }
            return score;
        }
    }
    /**
     * Permet d'évaluer le plateau de jeu
     * @param plateau plateau du jeu
     * @return score du plateau
     */
    public int evaluatePlateau(Case[][] plateau){
        int nbPionsVulnerables = 0;
        int nbPionsAAttaque = 0;
        int score = 0;

        NbPions nbPions = JoueurTowa.nbPions(plateau);
        if(couleur==Case.CAR_NOIR) {
            return nbPions.nbPionsNoirs - nbPions.nbPionsBlancs;
        }
        return nbPions.nbPionsBlancs-nbPions.nbPionsNoirs;
    }

    /**
     * L'adversaire joue : on récupère son action, met à jour le plateau, et
     * signale toute disqualification.
     *
     * @param plateau le plateau de jeu
     * @param couleurAdversaire couleur de l'adversaire
     * @return l'action choisie sous forme de chaîne
     */
    boolean adversaireJoue(Case[][] plateau, char couleurAdversaire) {
        boolean disqualification = false;
        System.out.println("Attente de réception action adversaire...");
        String actionAdversaire = grandOrdo.recevoirAction();
        System.out.println("Action adversaire reçue : " + actionAdversaire);
        if ("Z".equals(actionAdversaire)) {
            System.out.println("L'adversaire est disqualifié.");
            disqualification = true;
        } else {
            System.out.println("L'adversaire joue : "
                    + actionAdversaire + ".");
            mettreAJour(plateau, actionAdversaire, couleurAdversaire);
        }
        return disqualification;
    }

    /**
     * Calcule la couleur du prochain joueur.
     *
     * @param couleurCourante la couleur du joueur courant
     * @return la couleur du prochain joueur
     */
    static char suivant(char couleurCourante) {
        return couleurCourante == Case.CAR_NOIR
                ? Case.CAR_BLANC : Case.CAR_NOIR;
    }

    /**
     * Mettre à jour le plateau suite à une action, supposée valide.
     *
     * @param plateau le plateau
     * @param action l'action à appliquer
     * @param couleurCourante couleur du joueur courant
     */
    static void mettreAJour(Case[][] plateau, String action,
            char couleurCourante) {
        // vérification des arguments
        if (plateau == null || action == null || action.length() != 3) {
            return;
        }
        Coordonnees coord = Coordonnees.depuisCars(action.charAt(1), action.charAt(2));
        switch (action.charAt(0)) {
            case 'P':
                poser(coord, plateau, couleurCourante);
                break;
            case 'A':
                activer(coord, plateau, couleurCourante);
                break;
            case 'F':
                fusionner(coord, plateau, couleurCourante);
                break;
            default:
                System.out.println("Type d'action incorrect : " + action.charAt(0));
        }
    }

    /**
     * Poser un pion sur une case donnée (vide ou pas).
     *
     * @param coord coordonnées de la case
     * @param plateau le plateau de jeu
     * @param couleurCourante couleur du joueur courant
     */
    static void poser(Coordonnees coord, Case[][] plateau, char couleurCourante) {
        Case laCase = plateau[coord.ligne][coord.colonne];
        if (laCase.tourPresente()) {
            laCase.hauteur++;
        } else {
            laCase.couleur = couleurCourante;
            if (ennemiVoisine(coord, plateau, couleurCourante)) {
                laCase.hauteur = 2;
            } else {
                laCase.hauteur = 1;
            }
        }
    }

    /**
     * Activer une tour.
     *
     * @param coord coordonnées de la case où se situe la tour à activer
     * @param plateau le plateau de jeu
     * @param couleurCourante couleur du joueur courant
     */
    static void activer(Coordonnees coord, Case[][] plateau, char couleurCourante) {
    Case caseCourante = plateau[coord.ligne][coord.colonne];
        List<Case> casesAPortee = caseVoisinActivation(plateau, coord, Utils.inverseCouleurJoueur(couleurCourante));


    for (Case tourADetruire : casesAPortee){
        if (tourADetruire.hauteur < caseCourante.hauteur) {
            detruireTour(tourADetruire);
        }
    }
}
    
    static void fusionner(Coordonnees coord, Case[][] plateau, char couleurCourante){
        Case caseCourante = plateau[coord.ligne][coord.colonne];
        int nbTours = 0;
        List<Case> casesAPortee = caseVoisinActivation(plateau, coord, couleurCourante);

        for (Case tourADetruire : casesAPortee){
            nbTours += tourADetruire.hauteur;
            detruireTour(tourADetruire);
        }
        caseCourante.hauteur = Math.min(4,caseCourante.hauteur+nbTours);
}

    /**
     * Détruire une tour.
     *
     * @param laCase la case dont on doit détruire la tour
     */
    static void detruireTour(Case laCase) {
        laCase.hauteur = 0;
        laCase.couleur = Case.CAR_VIDE;
    }

    /**
     * Indique si une case possède une case voisine avec une tour ennemie.
     *
     * @param coord la case dont on souhaite analyser les voisines
     * @param plateau le plateau courant
     * @param couleurCourante couleur du joueur courant
     * @return vrai ssi la case possède une voisine avec une tour ennemie
     */
    static boolean ennemiVoisine(Coordonnees coord, Case[][] plateau, char couleurCourante) {
        return voisines(coord)
                .map(v -> plateau[v.ligne][v.colonne])
                .filter(Case::tourPresente)
                .anyMatch(c -> c.couleur != couleurCourante);
    }

    /**
     * Les coordonnées des cases voisines dans le plateau.
     *
     * @param coord les coordonnées de la case d'origine
     * @return les coordonnées des cases voisines
     */
    static Stream<Coordonnees> voisines(final Coordonnees coord) {
        return IntStream.rangeClosed(-1, 1).boxed()
                .flatMap(l -> IntStream.rangeClosed(-1, 1)
                .filter(c -> !(l == 0 && c == 0))
                .mapToObj(c -> new Coordonnees(coord.ligne + l, coord.colonne + c)))
                .filter(v -> 0 <= v.ligne && v.ligne < Coordonnees.NB_LIGNES)
                .filter(v -> 0 <= v.colonne && v.colonne < Coordonnees.NB_COLONNES);
    }


    /**
     * Programme principal. Il sera lancé automatiquement, ce n'est pas à vous
     * de le lancer.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        System.out.println("Démarrage le " + format.format(new Date()));
        System.out.flush();
        // « create » du protocole du grand ordonnateur.
        final String USAGE
                = System.lineSeparator()
                + "\tUsage : java " + IATowa.class.getName()
                + " <hôte> <port> <ordre>";
        if (args.length != 3) {
            System.out.println("Nombre de paramètres incorrect." + USAGE);
            System.out.flush();
            System.exit(1);
        }
        String hote = args[0];
        int port = -1;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Le port doit être un entier." + USAGE);
            System.out.flush();
            System.exit(1);
        }
        int ordre = -1;
        try {
            ordre = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("L'ordre doit être un entier." + USAGE);
            System.out.flush();
            System.exit(1);
        }
        try {
            char couleurJoueur = (ordre == 1 ? 'N' : 'B');
            IATowa iaTowa = new IATowa(hote, port, couleurJoueur);
            iaTowa.connexion();
            iaTowa.toursDeJeu();
        } catch (IOException e) {
            System.out.println("Erreur à l'exécution du programme : \n" + e);
            System.out.flush();
            System.exit(1);
        }
    }
    
public static List<Case> caseVoisinActivation(Case[][] plateau, Coordonnees coord, char couleur) {
    // Stream for diagonal directions
    Stream<Case> diagonals = Stream.of(
            new Coordonnees(-1, -1), new Coordonnees(-1, 1),
            new Coordonnees(1, -1), new Coordonnees(1, 1))
            .map(d -> new Coordonnees(coord.ligne + d.ligne, coord.colonne + d.colonne))
            .filter(c -> estDansPlateau(c) && plateau[c.ligne][c.colonne].tourPresente() && plateau[c.ligne][c.colonne].couleur == couleur)
            .map(c -> plateau[c.ligne][c.colonne]);

    // Stream for straight directions
    Stream<Case> directions = Stream.of(new int[][] {{0, -1}, {1, 0}, {-1, 0}, {0, 1}})
            .map(dir -> verifDirectionCouleur(plateau, coord, dir, couleur, 1))
            .filter(Objects::nonNull);

    // Combine and collect results
    return Stream.concat(diagonals, directions).collect(Collectors.toList());
}
    
    public static Case verifDirectionCouleur(Case[][] plateau, Coordonnees coord, int[] direction, char couleur, int depart) {
    return IntStream.range(depart, Coordonnees.NB_LIGNES)
        .mapToObj(i -> new Coordonnees(coord.ligne + direction[1] * i, coord.colonne + direction[0] * i))
        .filter(IATowa::estDansPlateau)
        .map(c -> plateau[c.ligne][c.colonne])
        .filter(Case::tourPresente)
        .findFirst()
        .filter(c -> c.couleur == couleur)
        .orElse(null);
}
    
    public static boolean estDansPlateau(Coordonnees coord) {
        return coord.ligne < Coordonnees.NB_LIGNES && coord.colonne < Coordonnees.NB_COLONNES
                && coord.ligne >= 0 && coord.colonne >= 0;
    }

    public static String[] enleverVitaliteTableau(String[] actions){
        return Arrays.stream(actions)
                .map(ActionsPossibles::enleverVitalites)
                .toArray(String[]::new);
    }

    public static Case[][] copierPlateau(Case[][] plateau){
        Case[][] newPlateau = new Case[Coordonnees.NB_LIGNES][Coordonnees.NB_COLONNES];
        for(int i=0;i< plateau.length;i++){
            for(int j=0;j< plateau[0].length;j++){
                Case caseInitiale = plateau[i][j];
                Case copieCase = new Case(caseInitiale.couleur,caseInitiale.hauteur,0,Case.CAR_TERRE);
                newPlateau[i][j] = copieCase;
            }
        }
        return newPlateau;
    }

    public static String[] selectionnerActions(String[] actionsPossibles, char couleur) {
        if(NB_ACTION_SELEC == -1){
            return actionsPossibles;
        }
        PriorityQueue<Map.Entry<String, Integer>> meilleuresActions = new PriorityQueue<>(
                (a, b) -> Integer.compare(b.getValue(), a.getValue())
        );

        PriorityQueue<Map.Entry<String, Integer>> piresActions = new PriorityQueue<>(
                Comparator.comparingInt(Map.Entry::getValue)
        );

        for (String action : actionsPossibles) {
            int score = scoreAction(action, couleur);
            meilleuresActions.add(new AbstractMap.SimpleEntry<>(action, score));
            piresActions.add(new AbstractMap.SimpleEntry<>(action, score));
        }

        String[] actionsSelectionnees = new String[NB_ACTION_SELEC * 2]; // Pour stocker les 10 meilleures et les 10 pires

        for (int i = 0; i < NB_ACTION_SELEC; i++) {
            if (!meilleuresActions.isEmpty()) {
                actionsSelectionnees[i] = meilleuresActions.poll().getKey();
            }
            if (!piresActions.isEmpty()) {
                actionsSelectionnees[NB_ACTION_SELEC + i] = piresActions.poll().getKey();
            }
        }

        return actionsSelectionnees;
    }

    public static int[] getVitalite(String action){
        String[] parts = action.split(",");
        int[] numbers = new int[2];
        numbers[0] = Integer.parseInt(parts[1]);
        numbers[1] = Integer.parseInt(parts[2]);
        return numbers;
    }

    public static int scoreAction(String action,char couleur){
        int[] numbers = getVitalite(action);
        switch (couleur){
            case Case.CAR_NOIR:
                return numbers[0]-numbers[1];

            case Case.CAR_BLANC:
                return numbers[1]-numbers[0];

            default:
                return 0;
            }

        }

    

}
