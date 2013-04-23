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
	//ADDCND("My condition", "My category", "%o My condition", &ExtObject::cMyCondition, "MyCondition", 0);

	/////////////////////////////
	// Actions
	// Format:
	// ADDCND(List name, Category, Display string, Function address, Script name, Flags)
	ADDPARAM(PARAM_VALUE, "Blue portal X", "Horizontal position of the red portal.");
	ADDPARAM(PARAM_VALUE, "Blue portal Y", "Vertical position of the red portal.");
	ADDPARAM(PARAM_VALUE, "Blue portal Angle", "Angle of the red portal.");

	ADDPARAM(PARAM_VALUE, "Red portal X", "Horizontal position of the red portal.");
	ADDPARAM(PARAM_VALUE, "Red portal Y", "Vertical position of the red portal.");
	ADDPARAM(PARAM_VALUE, "Red portal Angle", "Angle of the red portal.");

	ADDACT("Set portal settings", "Portal Settings", "Set portal settings", &ExtObject::aPortalSettings, "PortalSettings", 0);

	ADDPARAM(PARAM_VALUE, "X position", "X position of the object to be portalized");
	ADDPARAM(PARAM_VALUE, "Y position", "Y position of the object to be portalized");
	ADDPARAM(PARAM_VALUE, "X speed", "X speed of the object to be portalized");
	ADDPARAM(PARAM_VALUE, "Y speed", "Y speed of the object to be portalized");
	ADDPARAM(PARAM_VALUE, "Angle", "Angle of the object to be portalized");

	ADDACT("Set object settings", "Object Settings", "Set portal objects speed to %0, %1", &ExtObject::aObjectSettings, "ObjectSettings", 0);

	ADDPARAMCOMBO("Set target portal", "Set the portal colour to project to", "Red|Blue");

	ADDACT("Set target portal", "Portal Settings", "Set the portal target to %0", &ExtObject::aTargetPortal, "SetTargetPortal", 0);


	/////////////////////////////
	// Expressions
	// ADDEXP(List name, Category, Display string, Function address, Flags)
	ADDEXP("Speed X", "Projected Values", "SpeedX", &ExtObject::eProjectedSpeedX, RETURN_FLOAT);
	ADDEXP("Speed Y", "Projected Values", "SpeedY", &ExtObject::eProjectedSpeedY, RETURN_FLOAT);
	ADDEXP("X", "Projected Values", "X", &ExtObject::eProjectedX, RETURN_FLOAT);
	ADDEXP("Y", "Projected Values", "Y", &ExtObject::eProjectedY, RETURN_FLOAT);
	ADDEXP("Angle", "Projected Values", "Angle", &ExtObject::eProjectedAngle, RETURN_FLOAT);

	// This line includes your common ACEs as specified in Main.h
#include "..\..\Common\CommonAceTable.hpp"
}


