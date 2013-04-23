// Include StdAfx
#include "StdAfx.h"
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

	/////////////////////////////
	// Conditions
	ADDPARAMCOMBO("Comparison", "Select how to compare the depth", "Equal to|Not equal to|Less than|Less or equal|Greater than|Greater or equal");
	ADDPARAM(PARAM_VALUE, "Depth", "Depth to compare to");
	ADDCND("Compare depth", "Size & Position", "%o Depth %0 %1", &ExtObject::cCompareDepth, "CompareDepth", 0);

	/////////////////////////////
	// Actions
	ADDPARAM(PARAM_VALUE, "Depth", "Enter the new depth");
	ADDACT("Set depth", "Size & Position", "Set depth to %0", &ExtObject::aSetDepth, "SetDepth", 0);

	ADDPARAM(PARAM_VALUE, "Yaw", "Enter new yaw rotation, in degrees.");
	ADDACT("Set yaw", "3D Angle", "Set yaw to %0 degrees", &ExtObject::aSetYaw, "SetYaw", 0);

	ADDPARAM(PARAM_VALUE, "Pitch", "Enter new pitch rotation, in degrees.");
	ADDACT("Set pitch", "3D Angle", "Set pitch to %0 degrees", &ExtObject::aSetPitch, "SetPitch", 0);

	ADDPARAM(PARAM_VALUE, "Roll", "Enter new roll rotation, in degrees.");
	ADDACT("Set roll", "3D Angle", "Set roll to %0 degrees", &ExtObject::aSetRoll, "SetRoll", 0);

	/////////////////////////////
	// Expressions
	ADDEXP("Get depth", "Size & Position", "Depth", &ExtObject::eGetDepth, RETURN_FLOAT);
	ADDEXP("Get yaw", "3D Angle", "Yaw", &ExtObject::eGetYaw, RETURN_FLOAT);
	ADDEXP("Get pitch", "3D Angle", "Pitch", &ExtObject::eGetPitch, RETURN_FLOAT);
	ADDEXP("Get roll", "3D Angle", "Roll", &ExtObject::eGetRoll, RETURN_FLOAT);
	ADDPARAM(PARAM_STRING, "Value", "Name of value.");
	ADDEXP("Get private variable", "Private variables", "Value", &ExtObject::eGetValue, 0);


#include "..\..\Common\CommonAceTable.hpp"
}


