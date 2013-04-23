// Main.cpp - Main implementation of object

// Include StdAfx

#include "StdAfx.h"

// If run only

#ifdef RUN_ONLY


//////////////////////////////////////////////////////////////////////////////////
// Conditions
//////////////////////////////////////////////////////////////////////////////////
long ExtObject::cOnFunction(LPVAL params)
{
	// Check if this is my function
	CString* pName = params[0].GetStringPtr();
	if (pName == NULL) return 0;

	return funcStack.back().name == *pName;
}

long ExtObject::cOnAnyFunction(LPVAL params)
{
	return true;
}

long ExtObject::cAlias(LPVAL params)
{
	// False until an event with OnAlias returns true
	aliasflag = false;

	CString aliasName = params[0].GetString();
	aliasName.MakeLower();

	bool inlineMode = params[1].GetBool();

	aliasCalls.push_back(aliasName);

	if (inlineMode) {
		pRuntime->SelectAll(pType);
		pRuntime->GenerateEventInline(3, this);		// On Alias (Inline)
		pRuntime->Unselect(this);
	}
	else {
		pRuntime->NewSOL();
		pRuntime->GenerateEvent(info.oid, 3, this);	// On Alias (Isolated)
		pRuntime->RestoreSOL();
	}

	aliasCalls.pop_back();

	return aliasflag;
}

long ExtObject::cOnAlias(LPVAL params)
{
	CString aliasName = params[0].GetString();
	aliasName.MakeLower();

	return aliasName == aliasCalls.back();
}

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::aAddParam(LPVAL params)
{
	curParams.push_back(params[0]);

	return 0;
}

long ExtObject::aClearParams(LPVAL params)
{
	curParams.resize(0);

	return 0;
}

long ExtObject::aCall(LPVAL params)
{
	

	CString* pName = params[0].GetStringPtr();
	if (pName == NULL) return 0;

	funcStack.push_back(FunctionStack());
	FunctionStack& f = funcStack.back();

	bool copySOL = params[1].GetBool();

	f.name = *pName;
	f.paramList = curParams;
	curParams.resize(0);

	if (copySOL)
		pRuntime->NewSOLCopy();		// A inherited, copied SOL layer
	else
		pRuntime->NewSOL();			// A blank, fresh SOL layer

	// Run the events
	pRuntime->GenerateEvent(info.oid, 1, this);		// On Any Function
	pRuntime->GenerateEvent(info.oid, 0, this);		// On Function

	// Pop back the SOL
	pRuntime->RestoreSOL();

	// Pop the func stack
	funcStack.pop_back();

	return 0;
}

long ExtObject::aCallDelayed(LPVAL params)
{
	delayedCalls.push_back(DelayedFunctionCall());
	DelayedFunctionCall& call = delayedCalls.back();

	call.funcName = params[0].GetString();
	call.scheduledTime = pRuntime->GetLayoutClock(pLayout) + params[1].GetInt();
	call.paramList = curParams;
	curParams.resize(0);

	return 0;
}

long ExtObject::aSetReturn(LPVAL params)
{
	returned = true;
	retVal = params[0];

	return 0;
}

long ExtObject::aAliasTrue(LPVAL params)
{
	aliasflag = true;

	return 0;
}

//////////////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////////////
long ExtObject::eGetNumParams(LPVAL params, ExpReturn& ret)
{
	if (funcStack.empty())
		return ret = 0;
	else
		return ret = funcStack.back().paramList.size();
}

long ExtObject::eGetParam(LPVAL params, ExpReturn& ret)
{
	if (funcStack.empty())
		return ret = 0;

	int index = params[0].GetInt() - 1;
	vector<ExpStore>& paramList = funcStack.back().paramList;

	if (index < 0 || index >= paramList.size()) {
		pRuntime->AddDebugLogMessage("Function object: Attempted to retrieve a non-existant parameter; returning 0");  
		return 0;
	}
	else return ret.ReturnCustom(pRuntime, paramList[index]);
}

long ExtObject::eGetFuncName(LPVAL params, ExpReturn& ret)
{
	if (funcStack.empty())
		return ret.ReturnString(pRuntime, "");

	return ret.ReturnString(pRuntime, funcStack.back().name);
}

long ExtObject::eGetReturn(LPVAL params, ExpReturn& ret)
{
	return ret.ReturnCustom(pRuntime, retVal);
}


#endif // #ifdef RUN_ONLY


//////////////////////////////////////////////////////////////////////////////////
// ACE tables
//////////////////////////////////////////////////////////////////////////////////

void DefineACES(MicroAceTime* at)
{
	/////////////////////////////
	// Conditions

	ADDPARAM(PARAM_STRING, "Function name", "The name of the function to call");
	ADDCND("On function", "Function", "On function <i>%0</i>", &ExtObject::cOnFunction, "OnFunction", CND_TRIGGERED);

	ADDCND("On any function", "Function", "On any function", &ExtObject::cOnAnyFunction, "OnAnyFunction", CND_TRIGGERED);

	ADDPARAM(PARAM_STRING, "Alias name", "Some text to identify the condition");
	ADDPARAMCOMBO("Object picking mode", "Choose whether objects picked by the alias are remembered back in this event", "Isolated|Inline");
	ADDCND("Call alias", "Condition aliasing", "%o %0 (%1)", &ExtObject::cAlias, "Alias", SOL_MODIFIER);

	ADDPARAM(PARAM_STRING, "Alias name", "Some text to identify this condition");
	ADDCND("On alias",	"Condition aliasing", "%o On Alias %0", &ExtObject::cOnAlias, "OnAlias", CND_TRIGGERED);

	/////////////////////////////
	// Actions
	// ADDACT(menuID, routine, scriptname, displaytext, flags)
	ADDPARAM(PARAM_UNDEFINED, "Value", "Enter a value for the parameter.");
	ADDACT("Add parameter", "Parameters", "Add parameter <i>%0</i>", &ExtObject::aAddParam, "AddParam", 0);

	ADDACT("Clear parameters", "Parameters", "Clear parameters", &ExtObject::aClearParams, "ClearParams", 0);

	ADDPARAM(PARAM_STRING, "Function name", "The name of the function to call.");
	ADDPARAMCOMBO("Remember picked objects?", "If yes, objects picked by this event's conditions are remembered in the 'On Function' event.", "Forget|Remember");
	ADDACT("Call function", "Function", "Call function <b>%0</b> (and %1 picked objects)", &ExtObject::aCall, "Call", 0);

	ADDPARAM(PARAM_STRING, "Function name", "The name of the function to call.");
	ADDPARAM(PARAM_VALUE, "Delay (ms)", "Enter the delay before calling the function, in milliseconds.");
	ADDACT("Call function after delay", "Function", "Call function <b>%0</b> after %1 ms", &ExtObject::aCallDelayed, "CallDelayed", 0);

	ADDPARAM(PARAM_UNDEFINED, "Value", "Enter the return value.");
	ADDACT("Set return value", "Function", "Set return value to %0", &ExtObject::aSetReturn, "SetReturn", 0);

	ADDACT("Return true", "Condition aliasing", "(Alias) Return true", &ExtObject::aAliasTrue, "AliasTrue", 0);

	/////////////////////////////
	// Expressions
	// ADDEXP(menuID, routine, expressionName, displaytext, flags)
	// Same format as conditions, except with the expression editor text.
	// expressionName = the name in the expression editor, e.g "Width" would
	// associate this expression when the user types YourObject.Width

	ADDEXP("Get number of parameters",	"Parameters",		"NumParams",	&ExtObject::eGetNumParams,	RETURN_VALUE);
	ADDPARAM(PARAM_VALUE, "Param number", "Number of the parameter to get.");
	ADDEXP("Get parameter",				"Parameters",		"Param",		&ExtObject::eGetParam,		0);
	ADDEXP("Get function name",			"Function",		"Name",			&ExtObject::eGetFuncName,	RETURN_STRING);
	ADDEXP("Get return value",			"Function",		"Return",		&ExtObject::eGetReturn,		0);
}





