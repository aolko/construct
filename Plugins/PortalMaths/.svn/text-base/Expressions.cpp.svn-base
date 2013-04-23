// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Built-in expressions
//////////////////////////////////////////////////////////////////////////////////

// Return the Default Value.  This is the value to be returned if your object name
// is used without an expression name, eg. in "Counter + 1".
// Parameters can be passed, eg. MyObject(3,7).  You can check ExpReturn::Type() or GetNumParams() to check for overloads.
long ExtObject::ReturnDefaultValue(LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

// Called for undefined expression names, if your extension allows it.
// Eg: MyObject.UndefinedExpression(3,4,5) calls this routine with
// "UndefinedExpression" as the expression name.  OF_UNDEFINEDEXPRESSIONS must be specified in Main.h.
long ExtObject::ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

cr::point ExtObject::projectSpeed()
{
	cr::point relativeSpeed = objectSpeed;
	relativeSpeed.rotate( target->angle - portal->angle);
	return relativeSpeed;

}

cr::point ExtObject::projectPosition()
{
	cr::point relativePosition = objectPosition - portal->position;
	relativePosition.rotate( target->angle - portal->angle);
	return target->position + relativePosition;
}


//////////////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::eProjectedSpeedX(LPVAL params, ExpReturn& ret)
{
	return ret = projectSpeed().x;
}

long ExtObject::eProjectedSpeedY(LPVAL params, ExpReturn& ret)
{
	return ret = projectSpeed().y;
}

long ExtObject::eProjectedX(LPVAL params, ExpReturn& ret)
{
	return ret = projectPosition().x;
}

long ExtObject::eProjectedY(LPVAL params, ExpReturn& ret)
{
	return ret = projectPosition().y;
}

long ExtObject::eProjectedAngle(LPVAL params, ExpReturn& ret)
{
	return cr::to_degrees( objectAngle - portal->angle + target->angle );
}