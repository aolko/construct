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

	/////////////////////////////
	// Actions
	// Format:
	// ADDCND(List name, Category, Display string, Function address, Script name, Flags)
	ADDPARAM(PARAM_VALUE, "X Position", "The horizontal position of the point to add in world units");
	ADDPARAM(PARAM_VALUE, "X Position", "The vertical position of the point to add in world units");
	ADDACT("Add point", "Ribbon", "Add point to %0, %1", &ExtObject::aAddPoint, "AddPoint", 0);

	ADDPARAM(PARAM_VALUE, "Number of points", "The number of points determine how many points are stored to make up the ribbon");
	ADDACT("Set number of points", "Ribbon", "Set number of points to %0", &ExtObject::aSetNumberOfPoints, "SetNumberOfPoints", 0);

	ADDPARAM(PARAM_VALUE, "Thickness", "The number of pixels wide the ribbon will be");
	ADDACT("Set ribbon thickness", "Ribbon", "Set the thickness of the ribbon to %0", &ExtObject::aSetThickness, "SetThickness", 0);

	ADDPARAM(PARAM_VALUE, "Segments", "Set the number of segments for the ribbon. The segments will spread out over the points. This number should equal the number of columns in the sprites distortion mesh.");
	ADDACT("Set number of segments", "Ribbon", "Set the number of segments to %0", &ExtObject::aSetSegments, "SetSegments", 0);


	/////////////////////////////
	// Expressions
	// ADDEXP(List name, Category, Display string, Function address, Flags)
	ADDPARAM(PARAM_VALUE, "Column", "0");
	ADDPARAM(PARAM_VALUE, "Row", "0");
	ADDEXP("Get distortion point X", "Sprite Distortion", "DistortX", &ExtObject::eDistortX, RETURN_INTEGER);
	
	ADDPARAM(PARAM_VALUE, "Column", "0");
	ADDPARAM(PARAM_VALUE, "Row", "0");
	ADDEXP("Get distortion point Y", "Sprite Distortion", "DistortY", &ExtObject::eDistortY, RETURN_INTEGER);

	ADDPARAM(PARAM_VALUE, "Column", "0");
	ADDPARAM(PARAM_VALUE, "Row", "0");
	ADDEXP("Get distortion point U", "Sprite Distortion", "DistortU", &ExtObject::eDistortU, RETURN_INTEGER);

	ADDPARAM(PARAM_VALUE, "Column", "0");
	ADDPARAM(PARAM_VALUE, "Row", "0");
	ADDEXP("Get distortion point V", "Sprite Distortion", "DistortV", &ExtObject::eDistortV, RETURN_INTEGER);

	// This line includes your common ACEs as specified in Main.h
#include "..\..\Common\CommonAceTable.hpp"
}


