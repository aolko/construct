// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::aPortalSettings(LPVAL params)
{
	blue.position.x = params[0].GetFloat();
	blue.position.y = params[1].GetFloat();
	blue.angle = cr::to_radians( params[2].GetFloat() );

	red.position.x = params[3].GetFloat();
	red.position.y = params[4].GetFloat();
	red.angle = cr::to_radians( params[5].GetFloat() );
	
	return 0;
}

long ExtObject::aObjectSettings(LPVAL params)
{
	objectPosition.x = params[0].GetFloat();
	objectPosition.y = params[1].GetFloat();
	objectSpeed.x = params[2].GetFloat();
	objectSpeed.y = params[3].GetFloat();
	objectAngle = cr::to_radians( params[4].GetFloat() );

	return 0;
}

long ExtObject::aTargetPortal(LPVAL params)
{
	// Get the example parameter
	int param = params[0].GetBool();

	if(param == 0)
	{
		portal = &blue;
		target = &red;
	}
	else
	{
		portal = &red;
		target = &blue;
	}

	return 0;
}
