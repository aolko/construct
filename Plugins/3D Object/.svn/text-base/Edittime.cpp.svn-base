// Edittime.cpp - Edittime implementation of object
//

// Include StdAfx and Properties.h
#include "StdAfx.h"

#ifndef RUN_ONLY

//////////////////////////////////////////////////////////////////////////////////
// Property table
//////////////////////////////////////////////////////////////////////////////////

// The property table defines the list of properties that appear in the property
// grid when your object is selected in the layout editor.
//
// See the SDK docs for full documentation, but to get you started:-
//
// PROPERTY_EDIT  (myVariable, "Label", "Description");		// A text field property.
// PROPERTY_VALUE(myVariable, "Label 2", "Description");	// An integer property.
// PROPERTY_FLOAT(myVariable, "Label 3", "Description");	// A double/float property
//
// The labels MUST be unique!

BEGIN_PROPERTY_TABLE();
	PROPERTY_FLOAT(depth, "Depth", "The depth of the box.");
	PROPERTY_FLOAT(z, "Z", "The Z depth of the box.");
	PROPERTY_FLOAT(scale, "Import Scale", "Scale the entire model by this when its loaded");
PROPERTY_NEW_CATEGORY("3D Angle", "Edit the cube textures.");
	PROPERTY_FLOAT(yaw, "Yaw", "The yaw of the 3d model.");
	PROPERTY_FLOAT(pitch, "Pitch", "The pitch of the 3d model.");
	PROPERTY_FLOAT(roll, "Roll", "The roll of the 3d model.");
PROPERTY_NEW_CATEGORY("Textures", "Edit the cube textures.");
	PROPERTY_BUTTON("Edit", "Front face", "Edit the texture for the front face", EditTexture());
PROPERTY_NEW_CATEGORY("Model", "The path to load the obj from");
	PROPERTY_EDIT  (filepath, "Path", "The location of the obj file");		// A text field property.
	//PROPERTY_BUTTON("Open", "Browse", "Open the file selector", BrowsePath());

	if (iMode == MODE_READ) 
		if (SLabel == "Path" )
			editObject->objectPtr->LoadModel();


END_PROPERTY_TABLE  ();


/////////////////////////////////////////////////////////////////////////////////
// Edittime functions
//////////////////////////////////////////////////////////////////////////////////

// Constructor: when your object is created during edittime or application loaded.
EditExt::EditExt(VEditTime* pVEditTime, editInfo* pEInfo)
{
	// Save the edittime and einfo pointers
	pEditTime = pVEditTime;
	pInfo = pEInfo;
	drawInit = false;
	pDevice = NULL;
}

void EditExt::EditTexture()
{
	pEditTime->EditImage(imgTexture);
}

void EditExt::BrowsePath()
{
	return;
}

void EditExt::LoadModel()
{
	FreeBuffers();
	myobject.objs.clear();

	myobject.load_from_file( filepath.GetBuffer() );
	UpdateVertexData();
}

// Destructor: when your object is deleted or the application closed:
// close any allocated resources here
EditExt::~EditExt()
{
	FreeBuffers();
}

void EditExt::FreeBuffers()
{
	for(vector<obj>::iterator o = myobject.objs.begin(); o!= myobject.objs.end(); o++)
	{
		IDirect3DVertexBuffer9* pVertices = (IDirect3DVertexBuffer9*)o->vertexBuffer;
		IDirect3DIndexBuffer9* pIndexes= (IDirect3DIndexBuffer9*)o->indexBuffer;
		if (pVertices) pVertices->Release();
		if (pIndexes) pIndexes->Release();

		o->vertexBuffer = NULL;
		o->indexBuffer = NULL;
	}
}

// Initialization:  when the frame editor is opened.  Create textures, fonts,
// and anything else DirectX volatile here.
void EditExt::Initialize()
{
	LoadModel();
}

// Called when any property has been changed.
void EditExt::OnPropertiesUpdate()
{
}

// When the object is inserted to the frame editor by the user.
void EditExt::OnPut()
{
	pInfo->objectWidth = 64;
	pInfo->objectHeight = 64;
	pInfo->originX = 0.5;
	pInfo->originY = 0.5;

	// Add an image for each face of the cube
	imgTexture = pEditTime->AddImage(256,256);

	depth = 64.0f;
	z = 0.0f;
	scale = 1;

	yaw = pitch = roll = 0;

	filepath = "";
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
	return TRUE;
}

// Update vertices for the object position
void EditExt::UpdateVertexData()
{
	if (pDevice == NULL)
		D3DInit();

	for( vector<obj>::iterator o = myobject.objs.begin(); o!= myobject.objs.end(); o++)
	{
		IDirect3DVertexBuffer9* pVertices = (IDirect3DVertexBuffer9*)o->vertexBuffer;
		IDirect3DIndexBuffer9* pIndexes= (IDirect3DIndexBuffer9*)o->indexBuffer;
	
		if( pVertices )
		{
			pVertices->Release();
			pVertices = NULL;
			o->vertexBuffer = NULL;
		}
		if( pIndexes )
		{
			pIndexes->Release();
			pIndexes = NULL;
			o->indexBuffer = NULL;
		}

		int vertex_count =  o->points.size();
		int index_count = o->number_of_indexes;

		if( index_count == 0 || vertex_count == 0 )
			return;

		// Set up the cube vertices
		hr = pDevice->CreateVertexBuffer(sizeof(TLVERTEX) * vertex_count, D3DUSAGE_WRITEONLY, D3DFVF_TLVERTEX, D3DPOOL_DEFAULT,
			&pVertices, NULL);

		o->vertexBuffer = (void*)pVertices;

		if (FAILED(hr)) {
			MessageBox(NULL, "Failed to create vertex buffer", "3D Object Error", MB_OK | MB_ICONHAND);
			return;
		}

		// Set up the cube vertices
		hr = pDevice->CreateIndexBuffer(sizeof(unsigned short) * index_count, D3DUSAGE_WRITEONLY, D3DFMT_INDEX16, D3DPOOL_DEFAULT,
			&pIndexes, NULL);

		o->indexBuffer = (void*)pIndexes;

		if (FAILED(hr)) {
			MessageBox(NULL, "Failed to create index buffer", "3D Object Error", MB_OK | MB_ICONHAND);
			return;
		}


		// Do the vertices
		TLVERTEX* vertexData;
		pVertices->Lock(0, 0, (void**)&vertexData, 0);

		TLVERTEX* vxDest = vertexData;
		TLVERTEX* vxDestEnd = vxDest + vertex_count;

		for (int i = 0 ; vxDest != vxDestEnd; vxDest++, i++) {
			vxDest->x = o->points[i].v->x;
			vxDest->y = o->points[i].v->y;
			vxDest->z = o->points[i].v->z;
			vxDest->u = o->points[i].vt->x;
			vxDest->v = o->points[i].vt->y;
			vxDest->colour = 0xffffffff; //white
		}
		pVertices->Unlock();

		// Do the indexes
		unsigned short* indexData;
		pIndexes->Lock(0, 0, (void**)&indexData, 0);

		unsigned short* ixDest = indexData;
		unsigned short* ixDestEnd = ixDest + index_count; 


		// Now add all the indexes
		list<obj_object>::iterator oo = o->objs.begin();
		for( ; oo!= o->objs.end(); oo++)
		{
			// set the texture etc...
			vector<obj_face>::iterator f = oo->faces.begin();
			for( ; f!= oo->faces.end(); f++)
			{
				vector<int>::iterator i = f->indexes.begin();
				for( ; i!= f->indexes.end(); i++)
				{
					*ixDest = *i;
					ixDest ++;
				}
			}
		}
	}

}



void EditExt::CreateZBuffer()
{
	// Z buffer not yet created or lost in lost device
	if (!pEditTime->LayoutKeyExists("zbuffer") || pEditTime->GetLayoutKey("zbuffer") == NULL) {

		SIZE s;
		s.cx = GetSystemMetrics(SM_CXSCREEN);
		s.cy = GetSystemMetrics(SM_CYSCREEN);

		hr = pDevice->CreateDepthStencilSurface(s.cx, s.cy, D3DFMT_D16, D3DMULTISAMPLE_NONE, 0, TRUE, &pDepthStencil, NULL);

		if (FAILED(hr)) {
			MessageBox(NULL, "3D Cube object: Failed to create depth stencil", "3D Cube", MB_OK | MB_ICONHAND);
			return;
		}

		hr = pDevice->SetDepthStencilSurface(pDepthStencil);

		if (FAILED(hr)) {
			MessageBox(NULL, "3D Cube object: Failed to set depth stencil", "3D Cube", MB_OK | MB_ICONHAND);
			return;
		}

		hr = pDevice->Clear(0, NULL, D3DCLEAR_ZBUFFER, 0, 1.0f, 0);

		if (FAILED(hr)) {
			MessageBox(NULL, "3D Cube object: Failed to clear depth stencil", "3D Cube", MB_OK | MB_ICONHAND);
			return;
		}

		pEditTime->SetLayoutKey("zbuffer", pDepthStencil, false);
		pEditTime->SetLayoutKey("zbufferclearframe", 0, false);
	}
	else
		pDepthStencil = (IDirect3DSurface9*)pEditTime->GetLayoutKey("zbuffer");
}

// Startup D3D resources
void EditExt::D3DInit()
{
	// Get the D3D device used by the runtime
	pDevice = (IDirect3DDevice9*)pEditTime->GetDisplayDevice();

	if (pDevice == NULL) {
		MessageBox(NULL, "No DirectX device available", "3D Cube Error", MB_OK | MB_ICONHAND);
		return;
	}

	// Set up the cube vertices
	for(vector<obj>::iterator o = myobject.objs.begin(); o!= myobject.objs.end(); o++)
	{
		o->vertexBuffer = NULL;
		o->indexBuffer = NULL;
	}

	//RECT Rect;
	//GetClientRect(pEditTime->GetFrameHwnd(), &Rect);

	SIZE s;
	s.cx = GetSystemMetrics(SM_CXSCREEN);
	s.cy = GetSystemMetrics(SM_CYSCREEN);

	//float dx = (s.cx - Rect.right) / s.cx;
	//float dy = (s.cy - Rect.bottom) / s.cy;




	//Here we build our View Matrix, think of it as our camera.
	//First we specify that our viewpoint is 8 units back on the Z-axis
	eye_vector = D3DXVECTOR3( 0, 0, 8.0f );

	//We are looking towards the origin
	lookat_vector = D3DXVECTOR3( 0, 0, 0.0f );

	//The "up" direction is the positive direction on the y-axis
	up_vector = D3DXVECTOR3(0.0f,1.0f,0.0f);

	D3DXMatrixLookAtLH(&view_matrix,&eye_vector,
								   &lookat_vector,
								   &up_vector);


	aspect=((float)s.cx / (float)s.cy);

	D3DXMatrixPerspectiveFovLH(&projection_matrix, //Result Matrix
							  D3DX_PI/4,          //Field of View, in radians.
							  aspect,             //Aspect ratio
							  1.0f,               //Near view plane
							  100.0f );           //Far view plane

	D3DXMatrixIdentity(&worldMatrix);
	D3DXMatrixIdentity(&rotMatrix);
	D3DXMatrixIdentity(&transMatrix);
	D3DXMatrixIdentity(&scaleMatrix);

	// The vectors used in screen projection
	orig.x = 0;
	orig.y = 0;
	orig.z = 0;
	normal.x = 0;
	normal.y = 0;
	normal.z = 1;		// Normal facing the view

	CreateZBuffer();
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