// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::aBounce		(LPVAL theParams)
{
	// Invert speed - basic effect but works
	dx *= -1;
	dy *= -1;

	return 0;
}

long ExtObject::aStop			(LPVAL theParams)
{
	// Reset speed
	dx = 0;
	dy = 0;
	return 0;
}

long ExtObject::aSetMaxXFloorSpeed	(LPVAL theParams)
{
	max_floor_speed = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetMaxXAirSpeed	(LPVAL theParams)
{
	max_air_speed = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetMaxYSpeed	(LPVAL theParams)
{
	max_fall = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetFloorAcc		(LPVAL theParams)
{
	floor_acc = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetFloorDec		(LPVAL theParams)
{
	floor_dec = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetAirAcc		(LPVAL theParams)
{
	air_acc = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetAirDec		(LPVAL theParams)
{
	air_dec = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aJump(LPVAL theParams)
{
	float ga = RADIANS(grav_dir * 90);

	int cosga = cos(ga)*1.5f;
	int singa = sin(ga)*1.5f;

	if(singa)
		dy = -jump_strength * singa;
	if(cosga)
		dx = -jump_strength * cosga;

	return 0;
}

long ExtObject::aSetInvertControl(LPVAL theParams)
{
	inv = theParams[0].GetBool();
	return 0;
}

long ExtObject::aSetIgnoreInput(LPVAL theParams)
{
	ignoringInput = theParams[0].GetBool();
	return 0;
}

long ExtObject::aSetActivated(LPVAL theParams)
{
	activated = theParams[0].GetBool();

	if (activated)
		pRuntime->CallOnFrame(this);

	return 0;
}

long ExtObject::aSetJumpStrength(LPVAL theParams)
{
	jump_strength = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetJumpSustain(LPVAL theParams)
{
	jump_sustain = theParams[0].GetInt();
	return 0;
}

long ExtObject::aSetGravity(LPVAL theParams)
{
	grav_fall = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetJumpGravity(LPVAL theParams)
{
	grav_jump = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetJumpSustainGravity(LPVAL theParams)
{
	grav_jump_sustain = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetGravDir(LPVAL theParams)
{
	grav_dir = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetXSpeed(LPVAL theParams)
{
	dx = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetYSpeed(LPVAL theParams)
{
	dy = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aSetLocalXSpeed(LPVAL theParams)
{
	cr::point local = GetLocalSpeed();
	local.x = theParams[0].GetFloat();
	SetLocalSpeed(local);
	return 0;
}

long ExtObject::aSetLocalYSpeed(LPVAL theParams)
{
	cr::point local = GetLocalSpeed();
	local.y = theParams[0].GetFloat();
	SetLocalSpeed(local);
	return 0;
}


long ExtObject::aSetFallThroughPlatforms(LPVAL theParams)
{
	action_gothroughplatform = theParams[0].GetBool();
	return 0;
}
long ExtObject::aSetAdditionalXSpeed(LPVAL theParams)
{
	additionalSpeedX = theParams[0].GetFloat();
	return 0;
}
long ExtObject::aSetAdditionalYSpeed(LPVAL theParams)
{
	additionalSpeedY = theParams[0].GetFloat();
	return 0;
}

long ExtObject::aAddToIgnoreList(LPVAL theParams)
{
	for(int i=0;i<ignorelist.size();i++)
	{
		if (ignorelist[i]==theParams->GetObjectParam(pRuntime))
			return 0;
	}
	ignorelist.push_back(theParams->GetObjectParam(pRuntime));
	return 0;
}
//ADDPARAM(PARAM_OBJECT, "Object to Stop Ignoring", "Stop Ignoring platforms of this object type");
//ADDACT("Remove Object from ignore list", "Ignore", "Remove %0 from ignore list", &ExtObject::aRemoveFromIgnoreList, "RemoveFromIgnoreList", 0);

long ExtObject::aRemoveFromIgnoreList(LPVAL theParams)
{

	for(int i=0;i<ignorelist.size();i++)
	{
		if (ignorelist[i]==theParams->GetObjectParam(pRuntime))
		{
			vector<CRunObjType*>::iterator it=ignorelist.begin()+i;
			ignorelist.erase(it);
		}
	}
	return 0;
}
//ADDACT("Clear ignore list", "Ignore", "Clear ignore list", &ExtObject::aClearIgnoreList, "ClearIgnoreList", 0);

long ExtObject::aClearIgnoreList(LPVAL theParams)
{
	ignorelist.clear();
	return 0;
}