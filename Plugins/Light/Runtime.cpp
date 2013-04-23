// Runtime.cpp - Runtime implementation of object
//

// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Runtime functions
//////////////////////////////////////////////////////////////////////////////////
#ifdef RUN_ONLY

// ExtObject constructor:
// Only use for class initializer list.  Object initialisation must be done in OnCreate.
ExtObject::ExtObject(initialObject* editObject, VRuntime* pVRuntime)
: renderer(pVRuntime->pRenderer)
{
	pRuntime = pVRuntime;
	info.editObject = editObject;
}

void ExtObject::UpdateShadowList()
{
	if (pRuntime->GlobalKeyExists("ShadowList"))
		pShadowList = (vector<ShadowInfo>*)pRuntime->GetGlobalKey("ShadowList");
}

// This is called just after the constructor.  You should perform all loading here.
void ExtObject::OnCreate()
{
	bin ar;
	ar.attach(info.editObject->eData, info.editObject->eSize);

	int Version = 0;
	ar >> Version;

	ImageHandleInfo* imgHandle = pRuntime->LoadSerializedImageHandle(ar);

	
	COLORREF rgb;
	float shadowopacityf;

	if (Version >= 2) {
		ar >> rgb;
		ar >> shadowopacityf;
		ar >> shadowrange;

		shadow_filter = cr::color(rgb);
		shadow_filter.a = shadowopacityf;
	}
	else
		shadow_filter = info.editObject->eColor;

	ar.detach();

	info.curTexture = renderer->CreateTextureFromHandle(imgHandle);
	info.imgHandle = imgHandle;

	info.w = info.editObject->eWidth;
	info.h = info.editObject->eHeight;

	info.x = info.editObject->eX;
	info.y = info.editObject->eY;
	info.pInfo->filter = info.editObject->eColor;
	info.displayangle = info.angle = info.editObject->eAngle;

	POINT hs = pRuntime->GetImageCenterPoint((int)imgHandle);
	info.HotSpotX = hs.x;
	info.HotSpotY = hs.y;
	info.HotSpotAngle = atan2((float)hs.y, (float)hs.x);
	info.HotSpotDist = sqrt((float)(hs.x * hs.x + hs.y * hs.y));

	info.isFlipped = 0;
	info.isMirrored = 0;

	SetupPrivateVariableVector(pRuntime, this, privateVars);

	SIZE window = pRuntime->GetWindowSize();

	// Use a layout bounding box ie. always call draw()
	layoutBox.left = 0;
	layoutBox.top = 0;
	layoutBox.right = pLayout->width;
	layoutBox.bottom = pLayout->height;
	info.pDisplayBoxOverride = &layoutBox;

	//if (shadowTex == 0)
	//	shadowTex = pRuntime->AddTextureRT(window.cx, window.cy);

	if (pRuntime->GlobalKeyExists("shadowTex")) {
		shadow_tex = (TextureHandle)pRuntime->GetGlobalKey("shadowTex");
	}
	else {
		shadow_tex = renderer->CreateRenderTargetTexture(window.cx, window.cy);
		pRuntime->SetGlobalKey("shadowTex", (void*)shadow_tex);	// make key exist
	}

	pShadowList = NULL;

	UpdateShadowList();
	UpdateBoundingBox();
}

// Destructor: called when an instance of your object is destroyed.
ExtObject::~ExtObject()
{

}

// Called every frame for you to update your object if necessary
// Return values: 1 (do not call RTOnFrame again), 0 (continue calling)
// The frame call order goes like this:
// run events -> OnFrame -> Draw -> OnFrame2 -> run events -> ...
BOOL ExtObject::OnFrame()
{
	// Update hotspot
	POINT hs = pRuntime->GetImageCenterPoint((int)info.imgHandle);
	
	float xScale = info.w / info.curTexture->image_widthf;
	float yScale = info.h / info.curTexture->image_heightf;

	info.HotSpotX = ((float)hs.x) * xScale;
	info.HotSpotY = ((float)hs.y) * yScale;
	info.HotSpotAngle = atan2(info.HotSpotY, info.HotSpotX);
	info.HotSpotDist = sqrt(info.HotSpotX * info.HotSpotX + info.HotSpotY * info.HotSpotY);

	UpdateShadowList();

	return 0;
}

BOOL ExtObject::OnFrame2()
{
	return 1;
}

void ExtObject::ComputeVertices(const ShadowInfo& si, point* vertices)
{

	OBJHEADER& info = si.obj->info;
	
	// Davos method:
	vertices[0] = point(-0.5f, -0.5f);
	vertices[1] = point( 0.5f, -0.5f);
	vertices[2] = point( 0.5f,  0.5f);
	vertices[3] = point( -0.5f, 0.5f);

	// Transform it
	for(int i = 0; i < 4; i++)
	{
		vertices[i] *= point(info.w, info.h);
		vertices[i].rotate(cr::to_radians(info.displayangle));
		vertices[i] += point(info.x, info.y);
	}



	// Tigs original method:
	/*float old = info.displayangle;
	info.displayangle = 0.0f;
	pRuntime->UpdateBoundingBox(si.obj);
	RECTF box = info.box;
	info.displayangle = old;
	pRuntime->UpdateBoundingBox(si.obj);

	point& p1 = vertices[0];
	point& p2 = vertices[1];
	point& p3 = vertices[2];
	point& p4 = vertices[3];

	p1.x = box.left;
	p1.y = box.top;

	p2.x = box.right;
	p2.y = box.top;
	
	p3.x = box.left;
	p3.y = box.bottom;

	p4.x = box.right;
	p4.y = box.bottom;

	float angle = info.displayangle;

	if (angle == 0.0f) return;

	angle = RADIANS(-angle);

	const float w = box.right - box.left;
	const float h = box.bottom - box.top;

	const float sina = sin(angle);
	const float cosa = cos(angle);

	const float wcosa = w * cosa;
	const float wsina = w * sina;
	const float hsina = h * sina;
	const float hcosa = h * cosa;

	p2.x = p1.x + wcosa;
	p2.y = p1.y - wsina;

	p3.x = p1.x + hsina;
	p3.y = p1.y + hcosa;

	p4.x = p1.x + wcosa + hsina;
	p4.y = p1.y - wsina + hcosa;

	float dx = info.HotSpotDist * cos(cr::to_radians(info.displayangle) + info.HotSpotAngle) - info.HotSpotX;
	float dy = info.HotSpotDist * sin(cr::to_radians(info.displayangle) + info.HotSpotAngle) - info.HotSpotY;
	p1.x -= dx;		p1.y -= dy;
	p2.x -= dx;		p2.y -= dy;
	p3.x -= dx;		p3.y -= dy;
	p4.x -= dx;		p4.y -= dy;*/

}

int nextClockwiseVertex[] = {
	1,		// P1 -> P2
	3,		// P2 -> P4
	0,		// P3 -> P1
	2		// P4 -> P3
};

float GetAngleDifference(float start, float end)
{
   float cs = cos(start);
   float ce = cos(end);
   float ss = sin(start);
   float se = sin(end);

   float dot = cs*ce + ss*se;
   return acos(dot);
}

void ExtObject::DrawShadow(const ShadowInfo& si)
{
	int i;

	vector<point> vertices;
	vector<point> projections;

	if(si.shape == shape_rectangle)
	{
		vertices.resize(4);
		ComputeVertices(si, &vertices[0]);
	}
	else if(si.shape == shape_polygon)
	{
		for( int v = 0; v < si.vertexCount; v++ )
		{
			OBJHEADER& sinfo = si.obj->info;
			point pt = si.vertex[v];

			// Transform it
			pt *= point(sinfo.w, sinfo.h);
			pt.rotate(cr::to_radians(sinfo.displayangle));
			pt += point(sinfo.x, sinfo.y);

			vertices.push_back(pt);
		}
		
	}
	projections.resize( vertices.size());

	// Compute vertex projections
	for (i = 0; i < vertices.size(); i++) {
		const point& p = vertices[i];

		// Project this vertex away from light, casting the shadow
		float dx = p.x - info.x;
		float dy = p.y - info.y;

		float a = atan2(dy, dx);
		float d = sqrt(dx*dx + dy*dy);
		d *= *(si.pShadowDepth);		// Multiply by shadow depth

		// Convert back to cartesian
		projections[i].x = p.x + cos(a) * d;
		projections[i].y = p.y + sin(a) * d;
	}

	// Compute backfacing edges and construct polygons
	for (i = 0; i < vertices.size(); i++) {
		
		int next_i = (i + 1) % vertices.size();
		// Calculate normal for this edge
		const point& p = vertices[i];
		const point& next = vertices[next_i];
		const point& projected = projections[i];
		const point& nextprojected = projections[next_i];

		float normal_dx = p.x - next.x;
		float normal_dy = p.y - next.y;

		float normal = atan2(normal_dy, normal_dx) + PI_2;

		float mx = (p.x + next.x) / 2.0f;
		float my = (p.y + next.y) / 2.0f;

		float dx = mx - info.x;
		float dy = my - info.y;

		float angleToFace = atan2(dy, dx);

		// Draw arrow showing normal if it is backfacing
		if (GetAngleDifference(normal, angleToFace) < PI_2) {

			// Quad this vertex with the next one and projected points
			renderer->SetTexture(NULL);
			renderer->BeginBatchQuads(4, 6);
			renderer->AddIndex(0);
			renderer->AddIndex(1);
			renderer->AddIndex(2);
			renderer->AddIndex(3);
			renderer->AddIndex(2);
			renderer->AddIndex(1);

			renderer->AddVertex(cr::point3d(projected.x, projected.y, 0.0f), cr::origin, shadow_filter);
			renderer->AddVertex(cr::point3d(nextprojected.x, nextprojected.y, 0.0f), cr::origin, shadow_filter);
			renderer->AddVertex(cr::point3d(p.x, p.y, 0.0f), cr::origin, shadow_filter);
			renderer->AddVertex(cr::point3d(next.x, next.y, 0.0f), cr::origin, shadow_filter);

		}

	}
}

// Draw: Construct calls this when it wants you to draw your object.  You can
// leave it blank if your extension doesn't draw anything.
void ExtObject::Draw()
{
	// Clear and render to the shadowTex
	//pRuntime->SetRenderTarget(shadowTex);
	//pRuntime->ClearRT();

	// Draw light to shadowTex
	renderer->SetTexture(info.curTexture);

	/*
	float x = info.x;
	float y = info.y;

	if (info.HotSpotX != 0 ||  info.HotSpotY != 0)
	{
		x -= info.HotSpotDist * cos(info.HotSpotAngle+Rad(info.displayangle));
		y -= info.HotSpotDist * sin(info.HotSpotAngle+Rad(info.displayangle));
	}
	*/

	// Translate to screen coords
	//x -= pLayout->scrollX;
	//y -= pLayout->scrollY;

//	pRuntime->Blitrc(x, y, info.w, info.h, info.displayangle, info.filter);

	renderer->Quad_xywh(info.x, info.y, info.w, info.h, info.displayangle, point(info.HotSpotX, info.HotSpotY), info.pInfo->filter);

	// Render shadows
	if (pShadowList != NULL && !pShadowList->empty()) {

		vector<ShadowInfo>::iterator i = pShadowList->begin();
		vector<ShadowInfo>::iterator end = pShadowList->end();

		for ( ; i != end; i++)
		{
			if((bool)i->shadow->GetData(1,0))
				DrawShadow(*i);
		}

	}

	// Render shadowTex to display
	//pRuntime->RestoreRenderTarget();
	//pRuntime->SetTexture(shadowTex);
	//pRuntime->Blit(pLayout->scrollX, pLayout->scrollY);
}

// WindowProc:  called when a window message, or WM_COMMAND specifying your window,
// is sent to Construct.  You must call RegisterWindow() before Construct will send
// you messages for the given window.
LRESULT ExtObject::WindowProc(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam)
{
	return DefWindowProc(hWnd, uMessage, wParam, lParam);
}

BOOL ExtObject::PreTranslateMessage(MSG* msg)
{
	return 0;
}

// For global objects
void ExtObject::OnFrameChange(int oldFrame, int newFrame)
{
	// Do anything your global object needs when the frame changes
	// oldFrame is -1 on start of app, newFrame is -1 on end of app
}

void ExtObject::DebuggerUpdateDisplay(ExpStore*& pPrivateVars)
{
	if (!privateVars.empty())
		pPrivateVars = &(privateVars[0]); 
}

void ExtObject::OnDebuggerValueChanged(const char* name, const char* value)
{
	if (CheckDebugVariableChange(name, value, privateVars, this, pRuntime))
		return;
}

void ExtObject::Serialize(bin& ar)
{
	int Version = 1;
	SerializeVersion(ar, Version);
	// Implement saving/loading of your object
}

long ExtObject::GetData(int id, void* param)
{
	switch (id) {
	// If your extension uses a window, you can return it here
	case GET_WINDOW:
		break;
	}

	return 0;
}

long ExtObject::CallFunction(int id, void* param)
{
	return 0;
}

// Return the Default Value.  This is the value to be returned if your object name
// is used without an expression name, eg. in "Counter + 1".
// Parameters can be passed, eg. MyArray(3,7).
long ExtObject::ReturnDefaultValue(LPVAL theParams, ExpReturn& ret)
{
	if (theParams[0].Type() == EXPTYPE_VARIABLENAME) {
		return ret.ReturnCustom(pRuntime, privateVars[theParams[0].GetVariableIndex(pRuntime, pType)]);
	}
	else return ret = 0;
}

// Called for undefined expression names, if your extension allows it.
// Eg: MyObject.UndefinedExpression(3,4,5) calls this routine with
// "UndefinedExpression" as the expression name.
long ExtObject::ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

#else //ifdef RUN_ONLY

CRunObject* WINAPI RTCreateObject(bin& ar, VRuntime* pRuntime) 
{
	return NULL;
}

void WINAPI RTDestroyObject(ExtObject* object)
{
}

#endif