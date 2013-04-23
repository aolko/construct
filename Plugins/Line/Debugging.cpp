// Include StdAfx
#include "StdAfx.h"

// Called to update the debugger display.
// Use calls to pRuntime->AddDebuggerItem to display items.
void ExtObject::DebuggerUpdateDisplay(ExpStore*& pPrivateVars)
{
	if (!privateVars.empty())
		pPrivateVars = &(privateVars.front()); 
}

// Called when a debugger value has been changed.
void ExtObject::OnDebuggerValueChanged(const char* name, const char* value)
{
	// For convenience, we prepare these values with CString for operator==,
	// and precalculated integer and float conversions.
	CString label = name;
	__int64 iValue = _atoi64(value);
	double fValue = atof(value);

	// Example:
	// if (label == "My item")
	//     myValue = iValue;
	if (CheckDebugVariableChange(name, value, privateVars, this, pRuntime))
		return;
}