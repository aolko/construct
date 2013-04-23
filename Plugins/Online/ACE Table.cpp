// Include StdAfx
#include "StdAfx.h"

// Include any used common ACE definitions.
#ifdef RUN_ONLY
#include "..\..\Common\CommonAceDef.hpp"
#endif

//////////////////////////////////////////////////////////////////////////////////
// ACE table
//////////////////////////////////////////////////////////////////////////////////

void DefineACES(MicroAceTime* at)
{
	// Define your ACE tables here.
	// Use ADDPARAM before each ADDACT/ADDCND/ADDEXP to add parameters.
	// See the documentation for full details.

	// Note in the display string, %o is your object icon and %0-%9 make up parameters.
	// Note the script name should be a name suitable for the routine as if it were a member function in a
	// scripting language, eg. "Clear Stored Items" -> "ClearStoredItems"

	/////////////////////////////
	// Conditions
	// Format:
	// ADDCND(List name, Category, Display string, Function address, Script name, Flags)
	//ADDCND("OnDisconnect", "My category", "%o My condition", &ExtObject::cMyCondition, "MyCondition", CND_TRIGGERED);
	ADDCND("On disconnect", "Triggers", "%o On disconnect", &ExtObject::cOnDisconnect, "OnDisconnect", CND_TRIGGERED);
	ADDCND("On connection lost", "Triggers", "%o On connection Lost", &ExtObject::cOnConnectionLost, "OnConnectionLost", CND_TRIGGERED);
	ADDCND("On banned", "Triggers", "%o On banned", &ExtObject::cOnBanned, "OnBanned", CND_TRIGGERED);
	ADDCND("On connect", "Triggers", "%o On connect", &ExtObject::cOnConnect, "OnConnect", CND_TRIGGERED);
	ADDCND("On server full", "Triggers", "%o On server full", &ExtObject::cOnServerFull, "OnServerFull", CND_TRIGGERED);
	ADDCND("On player disconnect", "Triggers", "%o On player disconnect", &ExtObject::cOnPlayerDisconnect, "OnPlayerDisconnect", CND_TRIGGERED);
	ADDCND("On player connect", "Triggers", "%o On player connect", &ExtObject::cOnPlayerConnect, "OnPlayerConnect", CND_TRIGGERED);
	ADDPARAM(PARAM_STRING, "Message", "The text that was sent");
	ADDCND("On message", "Messages", "%o On message %0", &ExtObject::cOnMessage, "OnMessage", CND_TRIGGERED);
	ADDCND("On any message", "Messages", "%o On any message", &ExtObject::cOnAnyMessage, "OnAnyMessage", CND_TRIGGERED);
	ADDCND("On player here", "Triggers", "%o On player here", &ExtObject::cOnPlayerHere, "OnPlayerHere", CND_TRIGGERED);
	ADDCND("Is Server", "Hosting", "%o Is Server", &ExtObject::cIsServer, "IsServer", 0);
	/////////////////////////////
	// Actions
	// Format:
	// ADDCND(List name, Category, Display string, Function address, Script name, Flags)
	ADDPARAMDEF(PARAM_STRING, "Host", "The IP address of the host", "\"localhost\"");
	ADDPARAMDEF(PARAM_VALUE, "Host Port", "The port we are connecting to", "1234");
	ADDPARAMDEF(PARAM_VALUE, "Local Port", "The port we are listening on", "0");
	ADDPARAMDEF(PARAM_STRING, "Password", "Here is an example parameter.", "\"construct\"");
	ADDACT("Connect", "Client", "Connect to %0 on port %1 using local port %2 (password %3)", &ExtObject::aConnect, "Connect", 0);
	ADDACT("Disconnect", "Client", "Disconnect", &ExtObject::aDisconnect, "Disconnect", 0);
	ADDPARAM(PARAM_UNDEFINED, "Param", "Any number of string for the param");
	ADDACT("Add parameter", "Message", "Add parameter %0", &ExtObject::aAddParameter, "AddParameter", 0);
	ADDPARAM(PARAM_STRING, "Message", "The message that will be sent and recieved by others");
	ADDACT("Send message", "Message", "Send message %0", &ExtObject::aSendMessage, "SendMessage", 0);
	ADDPARAMDEF(PARAM_VALUE, "Host port", "The port we are listening on", "1234");
	ADDPARAMDEF(PARAM_STRING, "Password", "Here is an example parameter.", "\"construct\"");
	ADDPARAMCOMBO("Playing Mode", "Set the playing mode of the server. If you select player it means you become a player in the game, otherwise your a host and have no presence in the game", "Dedicated server|Player");
	ADDACT("Host server", "Server", "Host server on port %0 ( password: %1, playing mode: %2)", &ExtObject::aHostServer, "HostServer", 0);
	ADDACT("Forward message", "Server", "Forward message to other clients", &ExtObject::aForwardMessage, "ForwardMessage", 0);
	ADDPARAM(PARAM_STRING, "Message", "The message that will be sent and recieved by others");
	ADDPARAM(PARAM_VALUE, "PlayerID", "The player ID of who to send the message to");
	ADDACT("Send message to", "Message", "Send message %0 to %1", &ExtObject::aSendMessageTo, "SendMessageTo", 0);

	ADDPARAM(PARAM_STRING, "Variable", "The name of the player variable to use");
	ADDPARAM(PARAM_UNDEFINED, "Value", "The value to set the variable to");
	ADDACT("Set variable", "Player Variables", "Set variable %0 to %1", &ExtObject::aSetPlayerVariable, "SetPlayerVariable", 0);

	////////////////////////////
	// Expressions
	// ADDEXP(List name, Category, Display string, Function address, Flags)
	ADDEXP("Get message", "Message", "Message", &ExtObject::eMessage, RETURN_STRING);
	ADDEXP("Number of params", "Message", "NumberOfParams", &ExtObject::eNumberOfParams, RETURN_INTEGER);
	ADDPARAM(PARAM_VALUE, "1", "Parameter");
	ADDEXP("Get param", "Message", "Param", &ExtObject::eParam, 0);
	ADDEXP("Get IP", "Information", "IP", &ExtObject::eGetIP, RETURN_STRING);
	ADDEXP("Get player ID", "Information", "PlayerID", &ExtObject::eGetPlayerID, RETURN_INTEGER);
	ADDEXP("Get remote player ID", "Information", "RemotePlayerID", &ExtObject::eGetRemotePlayerID, RETURN_INTEGER);

	ADDEXP("Get number of players", "Information", "RemotePlayerID", &ExtObject::eGetNumberOfPlayers, RETURN_INTEGER);

	ADDPARAM(PARAM_VALUE, "1", "Index");
	ADDEXP("Lookup player ID from index", "Information", "PlayerIDFromIndex", &ExtObject::ePlayerIDFromIndex, RETURN_INTEGER);

	ADDPARAM(PARAM_VALUE, "Name", "The name of the variable");
	ADDEXP("Get player variable", "Player Variables", "PlayerVariable", &ExtObject::ePlayerVariable, 0);

	ADDPARAM(PARAM_VALUE, "Name", "The name of the variable");
	ADDEXP("Get remote player variable", "Player Variables", "RemotePlayerVariable", &ExtObject::eRemotePlayerVariable, 0);



#include "..\..\Common\CommonAceTable.hpp"
}


