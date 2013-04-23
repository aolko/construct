// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::aSetDepth(LPVAL params)
{
	depth = params[0].GetFloat();

	return 0;
}

long ExtObject::aSetYaw(LPVAL params)
{
	yaw = RADIANS(params[0].GetDouble());
	return 0;
}

long ExtObject::aSetPitch(LPVAL params)
{
	pitch = RADIANS(params[0].GetDouble());
	return 0;
}

long ExtObject::aSetRoll(LPVAL params)
{
	info.angle = params[0].GetDouble();
	pRuntime->UpdateBoundingBox(this);
	return 0;
}

long ExtObject::aLoadFromFile(LPVAL params)
{
	myobject.load_from_file(params[0].GetString().GetBuffer());
	//myobject.scale(scale);

	return 0;
}
long ExtObject::aLoadFromResource(LPVAL params)
{
	MessageBox(0, "Load from resource is not yet implemented in 3d object, sorry", 0, 0);
	return 0;
}
long ExtObject::aLoadFromString(LPVAL params)
{
	/*std::string text = params[0].GetString().GetBuffer();
	std::stringstream ss;
	ss << text;
	myobject.load_from_string(ss);
	myobject.scale(scale);*/
	return 0;
}