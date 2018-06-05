###############################################################################
#
# Copyright (c) 2017, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# author: Jean Maeght
#
# Projecteur
# Fichier .mod : modeles des donnees, des variables et des contraintes
#
###############################################################################


###############################################################################
# Parametre temporaire pour calculs intermediaires
# (il est souvent utile de disposer d'une petite variable pour un calcul,
# sachant qu'on n'a pas le droit de declarer une variable dans une boucle ou un if)
###############################################################################
param tempo;


###############################################################################
# Donnees postes
###############################################################################

set SUBSTATIONS;
param substation_horizon     {SUBSTATIONS} symbolic;
param substation_fodist      {SUBSTATIONS};
param substation_Vnomi       {SUBSTATIONS};
param substation_Vmin        {SUBSTATIONS};
param substation_Vmax        {SUBSTATIONS};
param substation_fault       {SUBSTATIONS};
param substation_curative    {SUBSTATIONS};
param substation_pays        {SUBSTATIONS} symbolic;
param substation_id          {SUBSTATIONS} symbolic;
param substation_description {SUBSTATIONS} symbolic;


###############################################################################
# Donnees noeuds
###############################################################################

set NOEUD;
param noeud_poste    {NOEUD} integer;
param noeud_CC       {NOEUD} integer; #num de Composante connexe, on ne travaille que sur la 0 (principale), les autres HS
param noeud_V0       {NOEUD};
param noeud_angl0    {NOEUD};
param noeud_injA     {NOEUD};
param noeud_injR     {NOEUD};
param noeud_fault    {NOEUD};
param noeud_curative {NOEUD};
param noeud_id       {NOEUD} symbolic;

#
# Consistance
#
check {n in NOEUD}: noeud_poste[n] in SUBSTATIONS;

#
# Donnees creees
#
param noeud_type{NOEUD} default 1; # PQ par defaut
param noeud_phase_nulle; # Noeud qui aura la phase fixee a zero. Prendre le noeud 400kV le plus maille


###############################################################################
# Donnees groupes
###############################################################################

set UNIT dimen 2; # [unit, noeud]
param unit_noeudpossible {UNIT} integer;
param unit_substation{UNIT} integer;
param unit_Pmin    {UNIT};
param unit_Pmax    {UNIT};
param unit_qP      {UNIT};
param unit_qp      {UNIT};
param unit_QP      {UNIT};
param unit_Qp      {UNIT};
param unit_PV      {UNIT} symbolic; # Indique si le groupe est en reglage de tension
param unit_Vc      {UNIT}; # Tension de consigne
param unit_Pc      {UNIT}; # Puisssance   active de consigne
param unit_Qc      {UNIT}; # Puisssance reactive de consigne
param unit_fault   {UNIT};
param unit_curative{UNIT};
param unit_id      {UNIT} symbolic;
param unit_nom     {UNIT} symbolic; # description
param unit_P0      {UNIT}; # Initial value of P (if relevant)
param unit_Q0      {UNIT}; # Initial value of Q (if relevant)

set UNIT_ID := setof{(g,n) in UNIT}g;

#
# Consistance
#
check {(g,n) in UNIT}: n  in NOEUD union {-1};
check {(g,n) in UNIT}: unit_substation[g,n] in SUBSTATIONS;
check {(g,n) in UNIT}:
  unit_Pmax[g,n] >= 0 &&
  unit_Pmax[g,n] >= unit_Pmin[g,n];
check {(g,n) in UNIT}: 
  unit_Qp[g,n] >= unit_qp[g,n] &&
  unit_QP[g,n] >= unit_qP[g,n]
#  && unit_Qp[g,n] >= unit_qP[g,n] # Pour prendre un rectangle inclu dans le trapeze, on doit verifier qP<=Qp (c'est necessaire mais pas suffisant)
  ;

#
# Donnees creees
#
param unit_Qmax{UNIT};
param unit_Qmin{UNIT};


###############################################################################
# Definition de domaines PQV pour les groupes
###############################################################################
set DOMAIN dimen 2 ;
param domain_coeffP {DOMAIN};
param domain_coeffQ {DOMAIN};
param domain_coeffV {DOMAIN};
param domain_RHS    {DOMAIN};
param domain_Vnomi  {DOMAIN};
param domain_idinternal {DOMAIN} symbolic;


###############################################################################
# Donnees consos (translation: conso is brief for consommation, which means load)
###############################################################################

set CONSO dimen 2; # [conso, noeud]
param conso_substation{CONSO} integer;
param conso_PFix      {CONSO};
param conso_QFix      {CONSO};
param conso_fault     {CONSO};
param conso_curative  {CONSO};
param conso_id        {CONSO} symbolic;
param conso_nom       {CONSO} symbolic;
param conso_p         {CONSO};
param conso_q         {CONSO};

#
# Consistance
#
check {(c,n) in CONSO}: n in NOEUD union {-1};
check {(c,n) in CONSO}: conso_substation[c,n] in SUBSTATIONS;


###############################################################################
# Donnees shunts
###############################################################################

set SHUNT dimen 2; # [shunt, noeud]
param shunt_noeudpossible {SHUNT} integer;
param shunt_substation    {SHUNT} integer;
param shunt_valmin        {SHUNT}; # Susceptance B en p.u. : faire B*100*V^2 pour obtenir des MVAr
param shunt_valmax        {SHUNT}; # Susceptance B en p.u. : faire B*100*V^2 pour obtenir des MVAr
param shunt_interPoints   {SHUNT}; # Points intermediaires : s'il y en a 0 c'est qu'on est soit sur min, soit sur max
param shunt_valnom        {SHUNT}; # Susceptance B en p.u. : faire B*100*V^2 pour obtenir des MVAr
param shunt_fault         {SHUNT};
param shunt_curative      {SHUNT};
param shunt_id            {SHUNT} symbolic;
param shunt_nom           {SHUNT} symbolic;
param shunt_P0            {SHUNT};
param shunt_Q0            {SHUNT};
param shunt_sections_count {SHUNT} integer;

#
# Consistance
#
check {(s,n) in SHUNT}: n  in NOEUD union {-1};
check {(s,n) in SHUNT}: shunt_substation[s,n] in SUBSTATIONS;


###############################################################################
# Donnees Tables des prises
###############################################################################

set TAPS dimen 2;
param tap_ratio    {TAPS};
param tap_x        {TAPS};
param tap_angle    {TAPS};
param tap_fault    {TAPS};
param tap_curative {TAPS};
#
# Donnees crees
#
set TAPTABLES := setof {(l,t) in TAPS} l;

#
# Consistance
#
check {(l,t) in TAPS}: l > 0 && t >= 0;


###############################################################################
# Donnees Ratio tap changers
###############################################################################
param regl_V_missing := -99999.0;
set REGL;
param regl_tap0     {REGL} integer;
param regl_table    {REGL} integer;
param regl_onLoad   {REGL} symbolic;
param regl_V        {REGL} ;
param regl_fault    {REGL};
param regl_curative {REGL};
param regl_id       {REGL} symbolic;

#
# Consistance
#
check {r in REGL}: regl_table[r] in TAPTABLES;
check {r in REGL}: (regl_table[r], regl_tap0[r]) in TAPS;


###############################################################################
# Donnees Phase tap changers
###############################################################################

set DEPH;
param deph_tap0     {DEPH} integer;
param deph_table    {DEPH} integer;
param deph_fault    {DEPH};
param deph_curative {DEPH};
param deph_id       {DEPH} symbolic;

#
# Consistance
#
check {d in DEPH}: deph_table[d] in TAPTABLES;
check {d in DEPH}: (deph_table[d], deph_tap0[d]) in TAPS;


###############################################################################
# Donnees quadripoles
###############################################################################

set QUAD dimen 3; # [quadripole, noeud origine, noeud extremite]
param quad_subor      	{QUAD} integer;
param quad_subex      	{QUAD} integer;
param quad_3wt          {QUAD};
param quad_R          	{QUAD};
param quad_X          	{QUAD};
param quad_Gor          {QUAD};
param quad_Gex          {QUAD};
param quad_Bor         	{QUAD};
param quad_Bex          {QUAD};
param quad_cstratio     {QUAD}; # Rapport fixe
param quad_ptrRegl      {QUAD} integer; # Numero du regleur
param quad_ptrDeph      {QUAD} integer; # Numero du dephaseur
param quad_Por          {QUAD};
param quad_Pex          {QUAD};
param quad_Qor          {QUAD};
param quad_Qex          {QUAD};
param quad_patl1        {QUAD}; # IMAP (borne sup courant)
param quad_patl2        {QUAD}; # IMAP (borne sup courant)
param quad_merged       {QUAD} symbolic; # Indique si le quadripole est frontiere
param quad_fault        {QUAD};
param quad_curative     {QUAD};
param quad_id           {QUAD} symbolic;
param quad_nom         	{QUAD} symbolic;

#
# Consistance
#
check {(qq,m,n) in QUAD}:
  m in NOEUD union {-1}
  && n in NOEUD union {-1}
  && ( m != n || m == -1 )
  && qq > 0
  && quad_subor[qq,m,n] in SUBSTATIONS
  && quad_subex[qq,m,n] in SUBSTATIONS;
check {(qq,m,n) in QUAD}: quad_ptrRegl[qq,m,n] in REGL union {-1};
check {(qq,m,n) in QUAD}: quad_ptrDeph[qq,m,n] in DEPH union {-1};

# Admittances
param quad_G {(qq,m,n) in QUAD} = +quad_R[qq,m,n]/(quad_R[qq,m,n]^2+quad_X[qq,m,n]^2);
param quad_B {(qq,m,n) in QUAD} = -quad_X[qq,m,n]/(quad_R[qq,m,n]^2+quad_X[qq,m,n]^2);

# Lignes a vide (Ytot = Yshunt + YlignesAvide)
param noeud_Ytot {k in NOEUD} =
  sum{(qq,k,-1) in QUAD} (
    quad_Bor[qq,k,-1] 
    + if (quad_Bex[qq,k,-1] == 0) 
      then 0 
      else 1/( cos(atan2(quad_R[qq,k,-1], quad_X[qq,k,-1]))*sqrt(quad_R[qq,k,-1]^2 + quad_X[qq,k,-1]^2) + 1/quad_Bex[qq,k,-1] )
  ) # Fin de la somme
  + sum{(qq,-1,k) in QUAD} (
    quad_Bex[qq,-1,k] 
    + if (quad_Bor[qq,-1,k] == 0) 
      then 0 
      else 1/( cos(atan2(quad_R[qq,-1,k], quad_X[qq,-1,k]))*sqrt(quad_R[qq,-1,k]^2 + quad_X[qq,-1,k]^2) +1/quad_Bor[qq,-1,k])
  );# Fin de la somme



###############################################################################
# Definition d'ensembles supplementaires
###############################################################################

# Noeuds dans la composante connexe principale
set NOEUDCC := {n in NOEUD : noeud_CC[n] == 0};
set CONSOCC := {(c,n) in CONSO  : n in NOEUDCC};
set SHUNTCC := {(s,n) in SHUNT  : n in NOEUDCC};
set QUADCC  := {(qq,m,n) in QUAD : m in NOEUDCC && n in NOEUDCC};
set QUADCC_REGL := {(qq,m,n) in QUADCC : quad_ptrRegl[qq,m,n] != -1 };
set QUADCC_DEPH := {(qq,m,n) in QUADCC : quad_ptrDeph[qq,m,n] != -1 };

# Groupes demarres : on refuse les cas Pc=Qc=0
set UNITCC  :=
  {(g,n) in UNIT : 
    n in NOEUDCC 
    and ( abs(unit_Pc[g,n]) > 0.0001 or abs(unit_Qc[g,n]) > 0.0001 ) 
  };

# Groupes qui sont en reglage de tension
set UNIT_PV  := setof {(g,n) in UNITCC: unit_PV[g,n]=="true" and unit_Vc[g,n]>0 } (g,n);

#
# Ensembles relatifs aux coupes definissant les domaines dynamiques
#
# Groupes ayant un domaine dynamique
set DOMAIN_IDENTIFIANTS := setof{(numero,id) in DOMAIN} id;
# Groupes connectes et demarres ayant un domaine dynamique (en 3 indices)
set UNIT_DOMAIN         := setof{(g,n) in UNITCC: unit_id[g,n] in DOMAIN_IDENTIFIANTS} (g,n,unit_id[g,n]);

# Nicolas Omont Juin 2017
# Eliminaton des groupes pour lesquels il y a un mismatch de tension entre l'etat de réseau et les domaines
param gen_vnom_mismatch{UNIT_DOMAIN} default 0;

# Groupes connectes et demarres ayant un domaine dynamique (en 1 seul indice)
set DOMAIN_ID           := setof{(g,n,gid) in UNIT_DOMAIN : gen_vnom_mismatch[g,n,unit_id[g,n]]==0} gid;
# Ensemble des contraintes des groupes connectes et demarres ayant un domaine dynamique (en 4 indices)
set UNIT_DOMAIN_CTR     := setof{(g,n,gid) in UNIT_DOMAIN,(numero,gid) in DOMAIN : gen_vnom_mismatch[g,n,unit_id[g,n]]==0} (numero,g,n,gid);


# Ensemble des groupes pour lesquels on faire varier P Q V :
# Jean Maeght + Nicolas Omont le 29 aout 2016 :
# Si aucun domaine dynamique n'a été fourni, alors on ne modifie pas le groupe
set UNIT_PQV := setof {(g,n) in UNITCC: unit_id[g,n] in DOMAIN_IDENTIFIANTS and gen_vnom_mismatch[g,n,unit_id[g,n]]==0} (g,n);


###############################################################################
# Prise en compte des parametres des regleurs et des dephaseurs
###############################################################################

param quad_angper {(qq,m,n) in QUADCC} =
  if (qq,m,n) in QUADCC_DEPH
  then atan2(quad_R[qq,m,n], tap_x[deph_table[quad_ptrDeph[qq,m,n]],deph_tap0[quad_ptrDeph[qq,m,n]]])
	else atan2(quad_R[qq,m,n], quad_X[qq,m,n]);

param quad_admi {(qq,m,n) in QUADCC} = 
  if (qq,m,n) in QUADCC_DEPH
  then 1./sqrt(quad_R[qq,m,n]^2 + tap_x[deph_table[quad_ptrDeph[qq,m,n]],deph_tap0[quad_ptrDeph[qq,m,n]]]^2)
  else 1./sqrt(quad_R[qq,m,n]^2 + quad_X[qq,m,n]^2);

param quad_Ror {(qq,m,n) in QUADCC} = 
	  ( if ((qq,m,n) in QUADCC_REGL)
      then tap_ratio[regl_table[quad_ptrRegl[qq,m,n]],regl_tap0[quad_ptrRegl[qq,m,n]]]
      else 1.0
    )
  * ( if ((qq,m,n) in QUADCC_DEPH)
      then tap_ratio[deph_table[quad_ptrDeph[qq,m,n]],deph_tap0[quad_ptrDeph[qq,m,n]]]
      else 1.0
    )
  * (quad_cstratio[qq,m,n]);
param quad_Rex {(q,m,n) in QUADCC} = 1; # Par convention dans iTesla, tout est cote 1, rien cote 2

param quad_dephor {(qq,m,n) in QUADCC} =
  if ((qq,m,n) in QUADCC_DEPH)
  then tap_angle [deph_table[quad_ptrDeph[qq,m,n]],deph_tap0[quad_ptrDeph[qq,m,n]]]
  else 0;
param quad_dephex {(qq,m,n) in QUADCC} = 0;


###############################################################################
# Bornes en tension
###############################################################################

param epsilon_tension_min = 0.5; # On considere qu'une Vmin ou Vmax < 0.5 ne vaut rien (exemple -99999)
param Vmin_defaut_horsgroupe := 0.80;
param Vmax_defaut_horsgroupe := 1.15;
param Vmin_defaut_groupe := 0.95;
param Vmax_defaut_groupe := 1.05;
param Vmin_defaut{n in NOEUDCC} :=
  if card({(g,n) in UNIT_PV}) > 0
  then Vmin_defaut_groupe
  else Vmin_defaut_horsgroupe;
param Vmax_defaut{n in NOEUDCC} :=
  if card({(g,n) in UNIT_PV}) > 0
  then Vmax_defaut_groupe
  else Vmax_defaut_horsgroupe;

# Noeuds pour lesquels on dispose d'une tension initiale
set NOEUDCC_V0 := {n in NOEUDCC : noeud_V0[n] > epsilon_tension_min};
check card(NOEUDCC_V0) == card(NOEUDCC);

/* # Jusqu'a Juin 2017
param max_noeud_V{n in NOEUDCC} := 
  if ( substation_Vmax[noeud_poste[n]] > epsilon_tension_min )
  then max( noeud_V0[n], substation_Vmax[noeud_poste[n]])
  else max( noeud_V0[n], Vmax_defaut[n]);
param min_noeud_V{n in NOEUDCC} :=
  if ( substation_Vmin[noeud_poste[n]] > epsilon_tension_min )
  then min( noeud_V0[n], substation_Vmin[noeud_poste[n]])
  else min( noeud_V0[n], Vmin_defaut[n]);
*/

# Juin 2017
param epsilon_V0 = 0.01;
param max_noeud_V{n in NOEUDCC} := 
  if ( substation_Vmax[noeud_poste[n]] > epsilon_tension_min )
  then max( noeud_V0[n]+epsilon_V0, substation_Vmax[noeud_poste[n]])
  else max( noeud_V0[n]+epsilon_V0, Vmax_defaut_horsgroupe);
param min_noeud_V{n in NOEUDCC} :=
  if ( substation_Vmin[noeud_poste[n]] > epsilon_tension_min )
  then min( noeud_V0[n]-epsilon_V0, substation_Vmin[noeud_poste[n]])
  else min( noeud_V0[n]-epsilon_V0, Vmin_defaut_horsgroupe);


# Ca peut paraitre bete de tester ceci, mais si on modifie le code ci-desssus,
# le check ci-desssous pourra rendre service
check {n in NOEUDCC} : min_noeud_V[n] < max_noeud_V[n];



###############################################################################
# Variables de tension et phase
###############################################################################

param Ph_min = -1 + min{n in NOEUDCC} noeud_angl0[n];
param Ph_max =  1 + max{n in NOEUDCC} noeud_angl0[n];

var Ph{n in NOEUDCC} >= Ph_min, <= Ph_max;
var V {n in NOEUDCC} <= max_noeud_V[n], >= min_noeud_V[n];


###############################################################################
# Bornes productions active et reactive
###############################################################################
param PQmax = 9000; # Toute valeur Pmin Pmax Qmin Qmax au dela de cette valeur sera invalidee
param Pmin_defaut := 0;
param Pmax_defaut := 200;
param ratioPmaxQmax := 0.4;

param Pmin {(g,n) in UNITCC} = 
  if abs(unit_Pmin[g,n]) > PQmax
  then min( unit_Pc[g,n], Pmin_defaut)
  #else unit_Pmin[g,n];
  else min( unit_Pc[g,n], unit_Pmin[g,n]);
param Pmax {(g,n) in UNITCC} = 
  if abs(unit_Pmax[g,n]) > PQmax
  then max( unit_Pc[g,n], Pmax_defaut)
  else max( unit_Pc[g,n], unit_Pmax[g,n]);

param Qmin {(g,n) in UNITCC} = 
  if abs(unit_qP[g,n]) > PQmax or abs(unit_qp[g,n]) > PQmax
  then min( unit_Qc[g,n], - ratioPmaxQmax * Pmax_defaut)
  else min( unit_Qc[g,n], unit_qP[g,n], unit_qp[g,n]);
param Qmax {(g,n) in UNITCC} = 
  if abs(unit_QP[g,n]) > PQmax or abs(unit_Qp[g,n]) > PQmax
  then max( unit_Qc[g,n], ratioPmaxQmax * Pmax_defaut)
  else max( unit_Qc[g,n], unit_QP[g,n], unit_Qp[g,n]);

check{(g,n) in UNITCC} : Pmin[g,n] <= Pmax[g,n];
check{(g,n) in UNITCC} : Qmin[g,n] <= Qmax[g,n];

set UNITHORSPMIN := setof {(g,n) in UNITCC : unit_Pc[g,n] < unit_Pmin[g,n]} unit_id[g,n] ;
set UNITHORSPMAX := setof {(g,n) in UNITCC : unit_Pc[g,n] > unit_Pmax[g,n]} unit_id[g,n];

###############################################################################
# Variables de production
###############################################################################
var unit_P {(g,n) in UNITCC} >= Pmin[g,n], <= Pmax[g,n];
var unit_Q {(g,n) in UNITCC} >= Qmin[g,n], <= Qmax[g,n];


# Inclusion dans un diagramme trapezoidal
param ctr_trapeze_qmax_rhs{(g,n) in UNIT_PQV} = Pmax[g,n] * unit_Qp[g,n] - Pmin[g,n] * unit_QP[g,n];
param ctr_trapeze_qmin_rhs{(g,n) in UNIT_PQV} = Pmin[g,n] * unit_qP[g,n] - Pmax[g,n] * unit_qp[g,n];
subject to ctr_trapeze_qmax{(g,n) in UNIT_PQV} :
    ( unit_Qp[g,n] - unit_QP[g,n] ) * unit_P[g,n]
  + ( Pmax[g,n]    - Pmin[g,n]    ) * unit_Q[g,n]
  <= ctr_trapeze_qmax_rhs[g,n];
subject to ctr_trapeze_qmin{(g,n) in UNIT_PQV} :
    ( unit_qP[g,n] - unit_qp[g,n] ) * unit_P[g,n]
  - ( Pmax[g,n]    - Pmin[g,n]    ) * unit_Q[g,n]
  <= ctr_trapeze_qmin_rhs[g,n];

# Definitions des domaines en (P,Q,V) pour la stabilite des modeles dynamiques
subject to ctr_domain{(numero,g,n,gid) in UNIT_DOMAIN_CTR : gen_vnom_mismatch[g,n,gid]==0 } :
    domain_coeffP[numero,gid] * unit_P[g,n]
  + domain_coeffQ[numero,gid] * unit_Q[g,n]
  + domain_coeffV[numero,gid] * substation_Vnomi[unit_substation[g,n]] * V[n]
  <= domain_RHS[numero,gid];


###############################################################################
# Transits 
###############################################################################

var Red_Tran_Act_Dir{(qq,m,n) in QUADCC} =
	+V[n]*quad_admi[qq,m,n]*quad_Ror[qq,m,n]*sin(Ph[m]-Ph[n]+quad_dephor[qq,m,n]-quad_angper[qq,m,n])
	+V[m]*quad_Ror[qq,m,n]^2*(quad_admi[qq,m,n]*sin(quad_angper[qq,m,n])+quad_Gor[qq,m,n])
;
var Red_Tran_Rea_Dir{(qq,m,n) in QUADCC} = 
	-V[n]*quad_admi[qq,m,n]*quad_Ror[qq,m,n]*cos(Ph[m]-Ph[n]+quad_dephor[qq,m,n]-quad_angper[qq,m,n])
	+V[m]*quad_Ror[qq,m,n]^2*(quad_admi[qq,m,n]*cos(quad_angper[qq,m,n])-quad_Bor[qq,m,n])
;

var Red_Tran_Act_Inv{(qq,m,n) in QUADCC} = 
	+V[m]*quad_admi[qq,m,n]*quad_Ror[qq,m,n]*sin(Ph[n]-Ph[m]-quad_dephor[qq,m,n]-quad_angper[qq,m,n])
	+V[n]*(quad_admi[qq,m,n]*sin(quad_angper[qq,m,n])+quad_Gex[qq,m,n])
;
var Red_Tran_Rea_Inv{(qq,m,n) in QUADCC} =
	-V[m]*quad_admi[qq,m,n]*quad_Ror[qq,m,n]*cos(Ph[n]-Ph[m]-quad_dephor[qq,m,n]-quad_angper[qq,m,n])
	+V[n]*(quad_admi[qq,m,n]*cos(quad_angper[qq,m,n])-quad_Bex[qq,m,n])
;



###############################################################################
# Bilans de puissance active en chaque noeud
###############################################################################

subject to bilan_P_noeud {k in NOEUDCC}: 
	  sum {(qq,k,n) in QUADCC} 100 * V[k] * Red_Tran_Act_Dir[qq,k,n]
	+ sum {(qq,m,k) in QUADCC} 100 * V[k] * Red_Tran_Act_Inv[qq,m,k]
	- sum {(g,k) in UNITCC} unit_P[g,k]
	= 
	- sum{(c,k) in CONSOCC} conso_PFix[c,k];

var term_bilan_P_noeud{k in NOEUDCC} =
	  sum {(qq,k,n) in QUADCC} 100 * V[k] * Red_Tran_Act_Dir[qq,k,n]
	+ sum {(qq,m,k) in QUADCC} 100 * V[k] * Red_Tran_Act_Inv[qq,m,k]
	- sum {(g,k) in UNITCC} unit_P[g,k];
var term_bilan_P_noeud_nul{k in NOEUDCC} =
	  sum {(qq,k,n) in QUADCC} 100 * V[k] * Red_Tran_Act_Dir[qq,k,n]
	+ sum {(qq,m,k) in QUADCC} 100 * V[k] * Red_Tran_Act_Inv[qq,m,k]
	- sum {(g,k) in UNITCC} unit_P[g,k]
	+ sum{(c,k) in CONSOCC} conso_PFix[c,k];



###############################################################################
# Bilans de puissance reactive en chaque noeud
###############################################################################
# Note sur la modelisation des shunts :
# shunt_noeud[k] doit etre multiplie par V[k]^2.
# Pour des raisons numeriques (=pour eviter d'avoir un carre de plus dans les 
# equations), on fait parfois comme si V[k]=Vnominale=1(pu).
# Sauf si gros probleme numerique, il faut mettre shunt_noeud[k] * V[k]^2

subject to bilan_Q_noeud {k in NOEUDCC}: 
	- 100 * noeud_Ytot[k]  * V[k]^2  
	- sum{(shunt,k) in SHUNTCC} 100 * shunt_valnom[shunt,k] * V[k]^2	
	+ sum{(qq,k,n)  in QUADCC}  100 * V[k] * Red_Tran_Rea_Dir[qq,k,n] 
	+ sum{(qq,m,k)  in QUADCC}  100 * V[k] * Red_Tran_Rea_Inv[qq,m,k] 
	- sum{(g,k) in UNITCC} unit_Q[g,k]
	=
	- sum{(c,k) in CONSOCC} conso_QFix[c,k];

var term_bilan_Q_noeud {k in NOEUDCC} =
	- 100 * noeud_Ytot[k]  * V[k]^2  
	- sum{(shunt,k) in SHUNTCC} 100 * shunt_valnom[shunt,k] * V[k]^2	
	+ sum{(qq,k,n)  in QUADCC}  100 * V[k] * Red_Tran_Rea_Dir[qq,k,n] 
	+ sum{(qq,m,k)  in QUADCC}  100 * V[k] * Red_Tran_Rea_Inv[qq,m,k] 
	- sum{(g,k) in UNITCC} unit_Q[g,k];
var term_bilan_Q_noeud_nul {k in NOEUDCC} =
	- 100 * noeud_Ytot[k]  * V[k]^2  
	- sum{(shunt,k) in SHUNTCC} 100 * shunt_valnom[shunt,k] * V[k]^2	
	+ sum{(qq,k,n)  in QUADCC}  100 * V[k] * Red_Tran_Rea_Dir[qq,k,n] 
	+ sum{(qq,m,k)  in QUADCC}  100 * V[k] * Red_Tran_Rea_Inv[qq,m,k] 
	- sum{(g,k) in UNITCC} unit_Q[g,k]
	+ sum{(c,k) in CONSOCC} conso_QFix[c,k];


###############################################################################
# Fonction objectif
###############################################################################

minimize somme_ecarts_quadratiques :
    sum {(g,n) in UNIT_PQV} ( unit_P[g,n] - unit_Pc[g,n] )^2
  + sum {(g,n) in UNIT_PQV} ( unit_Q[g,n] - unit_Qc[g,n] )^2
  + 10 * sum {(g,n) in UNIT_PV inter UNIT_PQV } ( unit_Pmax[g,n] * (V[n] - unit_Vc[g,n]) )^2;
