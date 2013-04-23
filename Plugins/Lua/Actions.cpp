// Include StdAfx
#include "StdAfx.h"
#include <iostream>

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////



long ExtObject::aDoString(LPVAL params)
{
	luaExtObject = this;
	// Get the example parameter
	std::string param = params[0].GetString();
	lua.dostring( param);

	// Do nothing
	return 0;
}

