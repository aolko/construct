// Main.h
// Definitions and classes for the object workings

// Only include once
#ifndef MAIN_H
#define MAIN_H

#define MINIMUM_VERSION	1
#define IDE_FLAGS 	OF_MOVEMENTS | OF_EFFECTS | OF_PRIVATEVARIABLES | OF_ALLOWANGLES | OF_DEFAULTVALUE_PRIVATEVAR
#include "..\..\Common\ExpReturn.hpp"
#include "..\..\Common\bin.h"

#define OBJECTRECT CRect(editObject->objectX, editObject->objectY, editObject->objectX + editObject->objectWidth, editObject->objectY + editObject->objectHeight)

#define COMMONACE_POSITION
#define COMMONACE_DIMENSION
#define COMMONACE_ANGLE
#define COMMONACE_ZORDER
#define COMMONACE_VISIBILITY
#define COMMONACE_OPACITY
#define COMMONACE_FILTER
#define COMMONACE_IDS
#define COMMONACE_COUNT_DESTROY

#define colshape int
#define shape_rectangle 0
#define shape_polygon 1

struct ShadowInfo {
	CRunObject* obj;
	CRunObject* shadow;
	float* pShadowDepth;
	colshape shape;
	int vertexCount;
	point* vertex;
};

#ifndef PI
#define PI 3.14159265358979323846f
#endif

#ifndef PI_2
#define PI_2 (PI / 2.0f)
#endif

//////////// RUNTIME OBJECT ////////////
// Add any member functions or data you want to this class.
// Your extension inherits CRunObject.  See the definition
// for the default member values you have access to.
class ExtObject : public CRunObject
{
public:
	// Constructor (called when Construct creates the object)
	ExtObject(initialObject* editObject, VRuntime* pVRuntime);
	// Destructor (called when Construct destroys the object)
	~ExtObject();

	IRenderer* const renderer;

	//////////////////////////
	// OnFrame: called once per frame just before Draw() (after the events list)
	// OnFrame2: called once per frame just after Draw() (before the events list)
	BOOL		OnFrame();
	BOOL		OnFrame2();
	// Draw: called when Construct wants you to render your object.
	void		Draw();
	// WindowProc is called if your extension creates a window and calls RegisterWindow().
	LRESULT		WindowProc(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam);
	BOOL		PreTranslateMessage(MSG* msg);
	long		ReturnDefaultValue(LPVAL theParams, ExpReturn& ret);
	long		ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret);
	void		OnCreate();
	// Misc functions
	void		OnFrameChange(int oldFrame, int newFrame);
	void		Serialize(bin& ar);
	// Data functions
	long		GetData(int id, void* param);
	long		CallFunction(int id, void* param);
	// Debugger
	void		DebuggerUpdateDisplay(ExpStore*& pPrivateVars);
	void		OnDebuggerValueChanged(const char* name, const char* value);

	////////////////////////////////////////////////////
	// ACTIONS, CONDITIONS AND EXPRESSIONS DEFINITIONS
#include "..\..\Common\CommonAceDecl.hpp"

	long cValueCmp(LPVAL theParams);

	long aSetValue(LPVAL theParams);
	long aAddValue(LPVAL theParams);
	long aSubValue(LPVAL theParams);
	long aDestroy(LPVAL theParams);

	long eGetValue(LPVAL theParams, ExpReturn& ret);

	////////////////////////////////////////////////////
	// Private values
	vector<ExpStore> privateVars;

	vector<ShadowInfo>* pShadowList;

	void UpdateShadowList();
	void DrawShadow(const ShadowInfo& si);
	void ComputeVertices(const ShadowInfo& si, point* vertices);

	TextureHandle shadow_tex;
	RECTF layoutBox;

	cr::color shadow_filter;
	float shadowrange;

};

//////////// EDITTIME INFO ////////////
class EditExt
{
public:

	///////////////////////////
	// Class data
	EditExt(class VEditTime* pVEditTime, class editInfo* pEInfo);
	~EditExt();

	void Draw();
	int  GetMenuIcons(int ACEMenu);
	void Initialize();
	void OnPut();
	void OnRemoved();
	int  OnShowACEMenu(TABLE ACEType);
	BOOL OnSizeObject();
	void Serialize(bin& ar);
	void OnPropertiesUpdate();
	void GetAnimationHandle(int&) {}
	BOOL OnNotify(int notify);

	class VEditTime* pEditTime;	// Pointer to Virtual Edittime
	class editInfo*  pInfo;		// Pointer to object edittime info

	///////////////////////////
	// Your edittime extension data goes here
	int imgTexture;
	int iTexture;

	unsigned long shadowfilter;
	float shadowopacityf;
	float shadowrange;



	void BtnEditImage();
};

// Internal stuff include
#include "..\..\Common\Internal.hpp"
#include "..\..\Common\Properties.h"

// Only include once
#endif