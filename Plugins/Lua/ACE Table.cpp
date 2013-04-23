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
	ADDPARAM(PARAM_STRING, "Trigger Name", "Name of the trigger");
	ADDCND("On Trigger", "Lua", "%o On %0", &ExtObject::cTrigger, "OnTrigger", CND_TRIGGERED);

	/////////////////////////////
	// Actions
	// Format:
	// ADDCND(List name, Category, Display string, Function address, Script name, Flags)
	ADDPARAM(PARAM_STRING, "Example parameter", "Here is an example parameter.");
	ADDACT("Do string", "Lua", "Do String (%0)", &ExtObject::aDoString, "DoString", 0);

	/////////////////////////////
	// Expressions
	// ADDEXP(List name, Category, Display string, Function address, Flags)
	ADDPARAM(PARAM_STRING, "name", "Variable name");
	ADDEXP("Get global value", "Lua", "GlobalValue", &ExtObject::eGetGlobalValue, RETURN_FLOAT);

	ADDPARAM(PARAM_STRING, "name", "Variable name");
	ADDEXP("Get global string", "Lua", "GlobalString", &ExtObject::eGetGlobalString, RETURN_FLOAT);

	ADDPARAM(PARAM_VALUE, "1", "Index");
	ADDEXP("Get param value", "Callback", "ParamValue", &ExtObject::eGetParamValue, RETURN_FLOAT);

	ADDPARAM(PARAM_VALUE, "1", "Index");
	ADDEXP("Get param string", "Callback", "ParamString", &ExtObject::eGetParamString, RETURN_FLOAT);

	// This line includes your common ACEs as specified in Main.h
#include "..\..\Common\CommonAceTable.hpp"
}


