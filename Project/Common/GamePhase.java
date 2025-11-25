package Project.Common;

// lsl8 | 11/24/25 | Tracks what part of the round we are in
public enum GamePhase {
    CHOOSING,   // players are picking
    RESOLVING,  // server is resolving battles
    ENDED       // round/session ended (ready for next)
}
