// Main.cpp - Main implementation of object
//

// Include StdAfx
#include "StdAfx.h"

// If run only
#ifdef RUN_ONLY

//////////////////////////////////////////////////////////////////////////////////
// Conditions
//////////////////////////////////////////////////////////////////////////////////



//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////
long ExtObject::aSetActivated(LPVAL theParams)
{
	activated = theParams[0].GetBool();

	return 0;
}

long ExtObject::aSetShadowDepth(LPVAL theParams)
{
	shadowDepth = theParams[0].GetFloat();

	return 0;
}

//////////////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////////////


#endif // #ifdef RUN_ONLY

//////////////////////////////////////////////////////////////////////////////////
// ACE tables
//////////////////////////////////////////////////////////////////////////////////

void DefineACES(MicroAceTime* at)
{
	// Define your ACE tables here.
	// Use ADDPARAM before each ADDACT/ADDCND/ADDEXP to add formal parameters.
	
	// Format:
	// ADDPARAM(type, title, description)
	//
	// type = the type of parameter.  See the PARAMS enum.
	// title = title of parameter, in expression editor
	// description = text to go in the text box describing this parameter.  Try to give
	//				 a helpful description.

	/////////////////////////////
	// Conditions

	// Format:
	// ADDCND(menuID, routine, scriptname, displaytext, flags)
	//
	// menuID = ID in the menu resource, must start at 2000
	// routine = address of the function associated
	// scriptname = reserved for future script use, e.g. "Set X position" script name could be "SetX"
	// displaytext = text to display in event editor, %0 is first param, %1 second etc
	// flags: can be
	// 0: ordinary evaluated event
	// CND_TRIGGERED: ordinary event trigger


	/////////////////////////////
	// Actions

	// ADDACT(menuID, routine, scriptname, displaytext)
	// Same format as ADDCND, but actions have no flags.

	ADDPARAMCOMBO("Activation", "Choose whether the movement is activated or deactivated", "Deactivate|Activate");
	ADDACT("Set activated", "Settings", "%0 shadow casting", &ExtObject::aSetActivated, "SetActivated", 0);

	ADDPARAM(PARAM_VALUE, "Shadow depth", "Depth of the shadow, eg. 1");
	ADDACT("Set shadow depth", "Settings", "Set shadow depth to %0", &ExtObject::aSetShadowDepth, "SetShadowDepth", 0);

	/////////////////////////////
	// Expressions

	// ADDEXP(menuID, routine, expressionName, displaytext, flags)
	// Same format as conditions, except with the expression editor text.
	// expressionName = the name in the expression editor, e.g "Width" would
	// associate this expression when the user types YourObject.Width
	// Flags:  RETURN_VALUE, RETURN_STRING, RETURN_UNDEFINED

}
