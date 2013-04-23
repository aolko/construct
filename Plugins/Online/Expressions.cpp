// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Built-in expressions
//////////////////////////////////////////////////////////////////////////////////

// Return the Default Value.  This is the value to be returned if your object name
// is used without an expression name, eg. in "Counter + 1".
// Parameters can be passed, eg. MyObject(3,7).  You can check ExpReturn::Type() or GetNumParams() to check for overloads.
long ExtObject::ReturnDefaultValue(LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

// Called for undefined expression names, if your extension allows it.
// Eg: MyObject.UndefinedExpression(3,4,5) calls this routine with
// "UndefinedExpression" as the expression name.  OF_UNDEFINEDEXPRESSIONS must be specified in Main.h.
long ExtObject::ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

//////////////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::eMessage(LPVAL params, ExpReturn& ret)
{
	return ret.ReturnString(pRuntime, message);
}
long ExtObject::eNumberOfParams(LPVAL params, ExpReturn& ret)
{
	return ret = paramList.size();
}
long ExtObject::eParam(LPVAL params, ExpReturn& ret)
{
	int index = params[0].GetInt() - 1;
	if (index < 0 || index >= messageParamList.size()) { 
		return 0;
	}
	else 
		return ret.ReturnCustom(pRuntime, messageParamList[index]);
}

long ExtObject::eGetIP(LPVAL params, ExpReturn& ret)
{
	return ret.ReturnString(pRuntime, raknet->GetLocalIP(0) );
}

long ExtObject::eGetPlayerID(LPVAL params, ExpReturn& ret)
{
	//return ret = raknet->GetGuidFromSystemAddress(RakNet::UNASSIGNED_SYSTEM_ADDRESS).systemIndex;
	//return ret = raknet->
	return ret = player->id; //ret.ReturnString(pRuntime, raknet->GetGuidFromSystemAddress(RakNet::UNASSIGNED_SYSTEM_ADDRESS).ToString());
}

long ExtObject::eGetRemotePlayerID(LPVAL params, ExpReturn& ret)
{
	if(remotePlayerID.size())
	{
		PlayerMap::iterator p = players.map.find(remotePlayerID.back() );
		if( p!= players.map.end())
			return ret = p->second->id;
		else
			return ret.ReturnString(pRuntime, remotePlayerID.back().ToString() );
	}
	return ret = 0;
}

long ExtObject::eGetNumberOfPlayers(LPVAL params, ExpReturn& ret)
{
	return ret = players.size();
}

long ExtObject::ePlayerIDFromIndex(LPVAL params, ExpReturn& ret)
{
	//PlayerMap::iterator p = players;
	return ret = 0;
}

long ExtObject::ePlayerVariable(LPVAL params, ExpReturn& ret)
{
	return ret = 0;
}

long ExtObject::eRemotePlayerVariable(LPVAL params, ExpReturn& ret)
{
	return ret = 0;
}
