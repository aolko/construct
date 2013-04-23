// Edittime.cpp - Edittime implementation of object
//

// Include StdAfx and Properties.h
#include "StdAfx.h"

#ifndef RUN_ONLY

//////////////////////////////////////////////////////////////////////////////////
// Property table
//////////////////////////////////////////////////////////////////////////////////

// The property table houses settings for your object that you wish to 
// be configurable before runtime
//
// For example, a button object would have settings relating to the text 
// that will be displayed, and what type of button it is
//
// There are several types of properties currently available to use:-
//
// PROPERTY_EDIT  , a text input field
// PROPERTY_STATIC, a static display of text
//
// Here is a brief example of them all:
//
// PROPERTY_EDIT  (myVariable, "Label", "Description);
// PROPERTY_STATIC(myVariable, "Label", "Description);

BEGIN_PROPERTY_TABLE();
/*
	PROPERTY_COLOR  (filter,   "Colour filter"  , "Tint the object a different color, white gives original colors");
	PROPERTY_PERCENT(opacityf, "Opacity", "Semitransparency of the object.");
	PROPERTY_FLOAT  (angle , "Angle" , "Orientation of the object, in degrees.");
	PROPERTY_BUTTON ("Edit", "Image", "Click to edit the object's image", BtnEditImage());
	PROPERTY_BOOL	(grabFrame, "Grab Layout", "Enable ability to process effects on the display underneath the canvas.");
	*/

	PROPERTY_COLOR  (shadowfilter,   "Shadow filter"  , "Tint the object a different color, white gives original colors");
	PROPERTY_PERCENT(shadowopacityf, "Shadow opacity", "Semitransparency of the object.")
	//PROPERTY_FLOAT	(shadowrange, "Shadow Range", "Specify in pixels how far away shadow casters can be (use -1 for infinity)");

	PROPERTY_BUTTON ("Edit", "Image", "Click to edit the light image", BtnEditImage());
END_PROPERTY_TABLE  ();


/////////////////////////////////////////////////////////////////////////////////
// Edittime functions
//////////////////////////////////////////////////////////////////////////////////

// Constructor: when your object is created during edittime or application loaded.
// Initialise values here.
EditExt::EditExt(VEditTime* pVEditTime, editInfo* pEInfo)
{
	// Save the edittime and einfo pointers
	pEditTime = pVEditTime;
	pInfo = pEInfo;



}

void EditExt::BtnEditImage()
{
	pEditTime->EditImage(imgTexture);
	iTexture = pEditTime->AddImageAsTexture(imgTexture);
}

// Destructor: when your object is deleted or the application closed:
// close any allocated resources here
EditExt::~EditExt()
{

}

void EditExt::OnPropertiesUpdate()
{

}

// Draw your object in the frame editor.
// Note the co-ordinates are automatically adjusted; draw at pInfo->objectX/Y
void EditExt::Draw()
{
	// Draw w/ hotspot
	iTexture = pEditTime->AddImageAsTexture(imgTexture);

	pEditTime->SetTexture(iTexture);
	POINT srcHotSpot = pEditTime->GetImageCenterPoint(imgTexture);
	SIZE imgsize  = pEditTime->GetImageSize(imgTexture);

	pInfo->originX = (float)(srcHotSpot.x) * pInfo->objectWidth / (float)max(0, imgsize.cx);
	pInfo->originY = (float)(srcHotSpot.y) * pInfo->objectHeight / (float)max(0, imgsize.cy);
	double HotSpotAngle = atan2(float(pInfo->originY), float(pInfo->originX));
	double HotSpotDist = sqrt((double)pInfo->originX * pInfo->originX + pInfo->originY * pInfo->originY);


	if (pInfo->originX == 0 && pInfo->originY == 0)
		pEditTime->Blitrc(pInfo->objectX, pInfo->objectY, pInfo->objectWidth, pInfo->objectHeight, (float)pInfo->objectAngle, pInfo->filter);
	else 
	{
		float dx = HotSpotDist * cos(cr::to_radians(pInfo->objectAngle) + HotSpotAngle);
		float dy = HotSpotDist * sin(cr::to_radians(pInfo->objectAngle) + HotSpotAngle);
		pEditTime->Blitrc(pInfo->objectX-dx, pInfo->objectY-dy, pInfo->objectWidth, pInfo->objectHeight, (float)pInfo->objectAngle, pInfo->filter);
	}
	
	pInfo->originX = (float)(srcHotSpot.x) / (float)max(1, imgsize.cx);
	pInfo->originY = (float)(srcHotSpot.y) / (float)max(1, imgsize.cy);
}

// Initialization:  when the frame editor is opened.  Create textures, fonts,
// and anything else DirectX volatile here.
void EditExt::Initialize()
{
	// Make a texture from the object's image
	iTexture = pEditTime->AddImageAsTexture(imgTexture);
}

// When the object is inserted to the frame editor by the user.
void EditExt::OnPut()
{
	USEMYRESOURCES();
	imgTexture = pEditTime->AddImageFromResource(AfxGetResourceHandle(), MAKEINTRESOURCE(IDR_DEFAULTLIGHT));
	pEditTime->EditImage(imgTexture);
	iTexture  = pEditTime->AddImageAsTexture(imgTexture);

	SIZE s;
	pEditTime->GetTextureSize(iTexture, &s);

	pInfo->objectWidth = s.cx;
	pInfo->objectHeight = s.cy;
	shadowfilter = 0xFF000000;
	shadowopacityf = 1;
	shadowrange = -1;

}


// When the object is removed from the frame editor by the user.
void EditExt::OnRemoved()
{

}

// Get any icons needed to show this ACEs menu
int EditExt::GetMenuIcons(int ACEMenu)
{
	// Todo: return the parameter number of a bitmap carrying an imagelist
	//       to show in the menu indicated by ACEMenu

	return -1; // -1 indicates no icons will be used
}


int EditExt::OnShowACEMenu(TABLE ACEType)
{
	// You can return the resource number indicating the menu to show for the
	//       ACE type indicated in ACEType
	// This overrides the default menu

	return -1;	// Return -1 for standard menu to be loaded
}

// On object resized
BOOL EditExt::OnSizeObject()
{
	// TRUE = allow sizing to occur, FALSE = disallow
	return TRUE;
}

void EditExt::Serialize(bin& ar)
{
	int Version = 2;
	SerializeVersion(ar, Version);

	if (ar.loading) {

		pEditTime->SerializeImage(imgTexture, ar);
		if(Version >= 2)
		{
			ar >> shadowfilter;
			ar >> shadowopacityf;
			ar >> shadowrange;
		}
		else{
			shadowfilter = pInfo->filter;
			shadowopacityf = (pInfo->filter >> 24) / 255.0f; 
			pInfo->filter = -1;
		}
	
	}
	else {
		// Code to save your object here
	
		pEditTime->SerializeImage(imgTexture, ar);
		ar << shadowfilter;
		ar << shadowopacityf;
		ar << shadowrange;

	}
}

BOOL EditExt::OnNotify(int notify)
{
	// Double clicked
	if(notify == 1)
	{
		BtnEditImage();
	}

	return TRUE;
}

#else //ifndef RUN_ONLY

BOOL WINAPI ETCreateObject(editInfo* editObject) {return FALSE;}
BOOL WINAPI ETInitializeObject(editInfo* editObject, VEditTime* pVEditTime) {return FALSE;}
BOOL WINAPI ETDestroyObject(editInfo* editObject) {return FALSE;}
int WINAPI ETDrawObject(editInfo *editObject) {return 0;}
void ETOnPropertyChanged(editInfo* editObject) {}
long LinkPropTable() {return 0;}
void WINAPI ETSerialize(editInfo* editObject, CArchive& ar) {}
void WINAPI ETPutObject(editInfo* editObject) {}
void WINAPI ETRemoveObject(editInfo* editObject) {}
void WINAPI GetInfo(LPOINFO objectInfo) {}
int WINAPI ETGetMenuIcons(editInfo* editObject, int ACEMenu) {return 0;}
int WINAPI ETShowACEMenu(editInfo* editObject, TABLE ACEType)  {return 0;}
BOOL WINAPI ETSizeObject(editInfo* editObject)  {return FALSE;}

#endif // RUN_ONLY

BOOL WINAPI ETOnNotify(editInfo* editObject, int notification)
{
#ifdef RUN_ONLY
	return 0;
#else
	return ((EditExt*)editObject->objectPtr)->OnNotify(notification);
#endif
}