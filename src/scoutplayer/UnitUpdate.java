package scoutplayer;

public enum UnitUpdate
{
    VERIFIED,   // an ally EC has been verified
    ENEMY,      // added to list of enemy ECs
    NEUTRAL,    // added to list of neutral ECs
    ENEMYTOALLY,    // Previously enemy EC now Ally
    ENEMYTONEUTRAL, // etc.
    ALLYTOENEMY, 
    ALLYTONEUTRAL,
    NETURALTOENEMY,
    NEUTRALTOALLY;
}
