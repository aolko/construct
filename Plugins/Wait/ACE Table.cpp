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
	ADDCND("Wait until", "Wait", "%o Until", &ExtObject::cWaitUntil, "WaitUntil", 0);

	ADDPARAM(PARAM_VALUE, "Delay (ms)", "The delay before the actions and subevents below are run");
	ADDCND("Delay (ms)", "Wait", "%o Delay %0 ms", &ExtObject::cDelay, "WaitDelay", 0);

	//---------
	ADDPARAM(PARAM_VALUE, "ID", "");
	ADDCND("", "Hidden", "%o My condition", &ExtObject::cWaitUntilActivated, "WaitUntilActivated", 0);

	ADDPARAM(PARAM_VALUE, "ID", "");	
	ADDCND("", "Hidden", "%o My condition", &ExtObject::cDelayComplete, "DelayComplete", 0);

	/////////////////////////////
	// Actions
	// Format:
	// ADDCND(List name, Category, Display string, Function address, Script name, Flags)
	ADDPARAM(PARAM_VALUE, "Delay (ms)", "The delay before the actions and subevents below are run");
	ADDACT("Delay (ms)", "Wait", "Delay %0 ms", &ExtObject::aWaitDelay, "WaitDelay2", 0);

	//---------
	ADDPARAM(PARAM_VALUE, "ID", "");
	ADDACT("", "Hidden", "Example action (%0)", &ExtObject::aActiveWaitDelay, "ActivateWaitDelay", 0);

	ADDPARAM(PARAM_VALUE, "ID", "");
	ADDACT("", "Hidden", "Example action (%0)", &ExtObject::aActivateWaitUntil, "ActiveWaitUntil", 0);

	ADDPARAM(PARAM_VALUE, "ID", "");
	ADDACT("", "Hidden", "Example action (%0)", &ExtObject::aDeactivateWaitUntil, "DeactivateWaitUntil", 0);


	/////////////////////////////
	// Expressions
	// ADDEXP(List name, Category, Display string, Function address, Flags)
	// ADDEXP("My expression", "My category", "MyExpression", &ExtObject::eMyExpression, RETURN_INTEGER);

	// This line includes your common ACEs as specified in Main.h
#include "..\..\Common\CommonAceTable.hpp"
}


