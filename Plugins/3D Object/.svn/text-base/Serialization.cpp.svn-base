// Include StdAfx.h
#include "StdAfx.h"

#ifdef RUN_ONLY

/////////////////////////////
// RUNTIME serialization
void ExtObject::Serialize(bin& ar)
{
	// TODO
	int Version = 1;
	SerializeVersion(ar, Version);

	if (ar.loading) {

		//

	}
	else {

		//

	}
}

#else // RUN_ONLY

/////////////////////////////
// EDITTIME serialization
// Save and load all your object's edittime data here.
void EditExt::Serialize(bin& ar)
{
	int Version = 1;
	SerializeVersion(ar, Version);

	if (ar.loading) {

		ar >> depth >> z >> pInfo->objectAngle >> scale;
		ar >> yaw >> pitch >> roll;
		ar >> filepath;

		pEditTime->SerializeImage(imgTexture, ar);
	}
	else {

		ar << depth << z << pInfo->objectAngle << scale;
		ar << yaw << pitch << roll;
		ar << filepath;
		
		pEditTime->SerializeImage(imgTexture, ar);
	}
}

#endif // RUN_ONLY