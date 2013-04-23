// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Conditions
//////////////////////////////////////////////////////////////////////////////////


void ExtObject::performTrigger()
{
	functionName = lua_tostring(lua.getState(), 1);
	pRuntime->GenerateEvent(info.oid, 0, this);
}

long ExtObject::cTrigger(LPVAL params)
{	
	if( params[0].GetString() == functionName )
		return true;
	else
		return false;
}
