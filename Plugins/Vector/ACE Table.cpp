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
	//ADDPARAM(PARAM_VALUE, "Example parameter", "Here is an example parameter.");
	//ADDACT("My action", "My category", "Example action (%0)", &ExtObject::aMyAction, "MyAction", 0);

	/////////////////////////////
	// Expressions
	// ADDEXP(List name, Category, Display string, Function address, Flags)
	
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "value", "");
	ADDEXP("Set X", "Set", "setX", &ExtObject::eSetX, RETURN_ARRAY);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "value", "");
	ADDEXP("Set Y", "Set", "setY", &ExtObject::eSetY, RETURN_ARRAY);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "value", "");
	ADDEXP("Set Length", "Set", "setLength", &ExtObject::eSetLength, RETURN_ARRAY);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "value", "");
	ADDEXP("Set Angle", "Set", "setAngle", &ExtObject::eSetAngle, RETURN_ARRAY);
	/////////////////////////////
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Get X", "Get", "X", &ExtObject::eGetX, RETURN_FLOAT);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Get Y", "Get", "Y", &ExtObject::eGetY, RETURN_FLOAT);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Get Length", "Get", "Length", &ExtObject::eGetLength, RETURN_FLOAT);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Get Angle", "Get", "Angle", &ExtObject::eGetAngle, RETURN_FLOAT);
	/////////////////////////////
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Add", "Operations", "Add", &ExtObject::eAdd, RETURN_ARRAY);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Subtract", "Operations", "Subtract", &ExtObject::eSubtract, RETURN_ARRAY);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Multiply", "Operations", "Multiply", &ExtObject::eMultiply, RETURN_ARRAY);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Divide", "Operations", "Divide", &ExtObject::eDivide, RETURN_ARRAY);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Dot", "Operations", "Dot", &ExtObject::eDot, RETURN_FLOAT);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_VALUE, "ratio", "");
	ADDEXP("Lerp", "Operations", "Lerp", &ExtObject::eLerp, RETURN_ARRAY);

	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDPARAM(PARAM_ARRAY, "vector", "");
	ADDEXP("Distance", "Operations", "Cross", &ExtObject::eDistance, RETURN_ARRAY);
	//////////////////////////////
	ADDEXP("Left", "Constants", "Left", &ExtObject::eLeft, RETURN_ARRAY);
	ADDEXP("Right", "Constants", "Right", &ExtObject::eRight, RETURN_ARRAY);
	ADDEXP("Up", "Constants", "Up", &ExtObject::eUp, RETURN_ARRAY);
	ADDEXP("Down", "Constants", "Down", &ExtObject::eDown, RETURN_ARRAY);
	ADDEXP("One", "Constants", "One", &ExtObject::eOne, RETURN_ARRAY);
	ADDEXP("Zero", "Constants", "Zero", &ExtObject::eZero, RETURN_ARRAY);













	// This line includes your common ACEs as specified in Main.h
#include "..\..\Common\CommonAceTable.hpp"
}


