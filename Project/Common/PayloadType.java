package Project.Common;

// lsl8 | 11/24/25 | new payload types added
public enum PayloadType {
    CLIENT_CONNECT,
    CLIENT_ID,
    DISCONNECT,
    MESSAGE,
    REVERSE,
    ROOM_CREATE,
    ROOM_JOIN,
    ROOM_LEAVE,
    SYNC_CLIENT,

    READY,       
    ROUND_START,    
    CHOICE_PICKED, 
    PICKED_NOTICE, 
    BATTLE_RESULT, 
    POINTS_SYNC,
    GAME_OVER 
}
