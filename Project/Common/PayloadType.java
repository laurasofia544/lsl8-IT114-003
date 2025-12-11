//lsl8 | 12/07/25 New added
package Project.Common;

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

    READY,          // ready up
    CHOICE_PICKED,  // /pick r|p|s
    ROUND_START,
    PICKED_NOTICE,
    BATTLE_RESULT,
    POINTS_SYNC,
    GAME_OVER,

    ROOM_LIST,
    SYNC_READY,
    RESET_READY,
    PHASE,
    TURN,
    SYNC_TURN,
    RESET_TURN,
    TIME,
    POINTS
}
