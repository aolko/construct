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

long ExtObject::eGetGlobalValue(LPVAL params, ExpReturn& ret)
{
	std::string param1 = params[0].GetString();
	return ret = lua.getGlobalFloat(param1);
}


long ExtObject::eGetGlobalString(LPVAL params, ExpReturn& ret)
{
	std::string param1 = params[0].GetString();
	ret.ReturnString(pRuntime, lua.getGlobalString(param1).c_str());
	return 1;
}

long ExtObject::eGetParamValue(LPVAL params, ExpReturn& ret)
{
	int index = params[0].GetInt();	
	float number = lua_tonumber(lua.getState(), index+1);
	return ret = number;
}
long ExtObject::eGetParamString(LPVAL params, ExpReturn& ret)
{
	int index = params[0].GetInt();	
	ret.ReturnString(pRuntime, lua_tostring(lua.getState(), index+1));
	return 1;
}