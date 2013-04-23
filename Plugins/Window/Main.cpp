// Main.cpp - Main implementation of object
//

// Include StdAfx
#include "StdAfx.h"

// If run only
#ifdef RUN_ONLY

// Auxiliary function
BOOL CALLBACK EnumerateWindows(HWND hWnd, LPARAM lParam)
{
    char TextBuffer[256];

	if (!hWnd)
        return TRUE; 

    ::GetWindowText(hWnd, TextBuffer, 255);

	CString Window = TextBuffer;
	Window.TrimLeft();
	Window.TrimRight();

	if (Window == "") return TRUE;

	((vector<ExpStore>*)lParam)->push_back((const char*)Window);

    return TRUE;
}

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::aAttach(LPVAL theParams)
{
	m_Window = (HWND)GetIntParam(theParams, 0);

	return 0;
}

long ExtObject::aCloseWindow(LPVAL theParams)
{
	CloseWindow(m_Window);

	return 0;
}

long ExtObject::aHideWindow(LPVAL theParams)
{
	ShowWindow(m_Window, SW_HIDE);

	return 0;
}

long ExtObject::aShowWindow(LPVAL theParams)
{
	ShowWindow(m_Window, SW_SHOW);

	return 0;
}

long ExtObject::aFlashWindow(LPVAL theParams)
{
	FlashWindow(m_Window, TRUE);

	return 0;
}

long ExtObject::aSetWindowTitle(LPVAL theParams)
{
	SetWindowText(m_Window, GetStringParam(theParams, 0));

	return 0;
}

long ExtObject::aMinimizeWindow(LPVAL theParams)
{
	ShowWindow(m_Window, SW_MINIMIZE);

	return 0;
}

long ExtObject::aRestoreWindow(LPVAL theParams)
{
	ShowWindow(m_Window, SW_RESTORE);

	return 0;
}

long ExtObject::aMaximizeWindow(LPVAL theParams)
{
	ShowWindow(m_Window, SW_MAXIMIZE);

	return 0;
}

long ExtObject::aAlwaysOnTop(LPVAL theParams)
{
	if (GetIntParam(theParams, 0) == 0)
	{
		SetWindowPos(m_Window, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOMOVE|SWP_NOSIZE);
		return 0;
	}
	else
	{
		SetWindowPos(m_Window, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE|SWP_NOSIZE);
		return 0;
	}

	return 0;
}

long ExtObject::aBringToTop(LPVAL theParams)
{
	BringWindowToTop(m_Window);

	return 0;
}

long ExtObject::aSetX(LPVAL theParams)
{
	RECT Rect;
	GetWindowRect(m_Window, &Rect);
	CRect WindowRect = Rect;

	SetWindowPos(m_Window, NULL, GetIntParam(theParams, 0), Rect.top, 0, 0, SWP_NOSIZE);

	return 0;
}

long ExtObject::aSetY(LPVAL theParams)
{
	RECT Rect;
	GetWindowRect(m_Window, &Rect);
	CRect WindowRect = Rect;

	SetWindowPos(m_Window, NULL, Rect.left, GetIntParam(theParams, 0), 0, 0, SWP_NOSIZE);

	return 0;
}

long ExtObject::aSetPosition(LPVAL theParams)
{
	SetWindowPos(m_Window, NULL, GetIntParam(theParams, 0), GetIntParam(theParams, 1), 0, 0, SWP_NOSIZE);

	return 0;
}

long ExtObject::aSetHeight(LPVAL theParams)
{
	RECT Rect;
	GetWindowRect(m_Window, &Rect);
	CRect WindowRect = Rect;

	SetWindowPos(m_Window, NULL, 0, 0, WindowRect.Width(), GetIntParam(theParams, 0), SWP_NOMOVE);

	return 0;
}

long ExtObject::aSetWidth(LPVAL theParams)
{	
	RECT Rect;
	GetWindowRect(m_Window, &Rect);
	CRect WindowRect = Rect;

	SetWindowPos(m_Window, NULL, 0, 0, GetIntParam(theParams, 0), WindowRect.Height(), SWP_NOMOVE);

	return 0;
}

long ExtObject::aSetSize(LPVAL theParams)
{
	SetWindowPos(m_Window, NULL, 0, 0, GetIntParam(theParams, 0), GetIntParam(theParams, 1), SWP_NOMOVE);

	return 0;
}

long ExtObject::aSetClientHeight(LPVAL theParams)
{
	RECT Rect;
	GetClientRect(m_Window, &Rect);
	CRect ClientRect = Rect;

	RECT WRect;
	GetWindowRect(m_Window, &WRect);
	CRect WindowRect = WRect;

	int delta = WindowRect.Height() - ClientRect.Height();

	SetWindowPos(m_Window, NULL, 0, 0, WindowRect.Width(), GetIntParam(theParams, 0) + delta, SWP_NOMOVE);

	return 0;
}

long ExtObject::aSetClientWidth(LPVAL theParams)
{
	RECT Rect;
	GetClientRect(m_Window, &Rect);
	CRect ClientRect = Rect;

	RECT WRect;
	GetWindowRect(m_Window, &WRect);
	CRect WindowRect = WRect;

	int delta = WindowRect.Width() - ClientRect.Width();

	SetWindowPos(m_Window, NULL, 0, 0, GetIntParam(theParams, 0) + delta, WindowRect.Height(), SWP_NOMOVE);

	return 0;
}

// Avoiding AdjustWindowRect here, because I couldn't validate if the window has a menu bar or not
long ExtObject::aSetClientSize(LPVAL theParams)
{
	RECT Rect;
	GetClientRect(m_Window, &Rect);
	CRect ClientRect = Rect;

	RECT WRect;
	GetWindowRect(m_Window, &WRect);
	CRect WindowRect = WRect;

	int deltaH = WindowRect.Width() - ClientRect.Width();
	int deltaV = WindowRect.Height() - ClientRect.Height();

	SetWindowPos(m_Window, NULL, 0, 0, GetIntParam(theParams, 0) + deltaH, GetIntParam(theParams, 1) + deltaV, SWP_NOMOVE);

	return 0;
}

long ExtObject::aAttachToCurrent(LPVAL theParams)
{
	m_Window = pRuntime->GetFrameHwnd(pLayout);

	return 0;
}

//////////////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::eFindWindow(LPVAL theParams, ExpReturn& ret)
{
	return ret = (int)FindWindow(NULL, GetStringParam(theParams, 0));
}

long ExtObject::eGetApplicationWindow(LPVAL theParams, ExpReturn& ret)
{
	return ret = (int)pRuntime->GetFrameHwnd(pLayout);
}

long ExtObject::eGetWindows(LPVAL theParams, ExpReturn& ret)
{
	Array.resize(0);
	EnumWindows(EnumerateWindows, (long)&Array);

	return ret.ReturnArray(&Array.front(), Array.size());
}

long ExtObject::eGetWindowTitle(LPVAL theParams, ExpReturn& ret)
{
	const int MAX_BUFFER = 256;
	char Buffer[MAX_BUFFER];
	GetWindowText(m_Window, Buffer, MAX_BUFFER);
	
	return ret.ReturnString(pRuntime, Buffer);
}

long ExtObject::eGetX(LPVAL theParams, ExpReturn& ret)
{
	RECT Rect;
	GetWindowRect(m_Window, &Rect);

	return ret = Rect.left;
}

long ExtObject::eGetY(LPVAL theParams, ExpReturn& ret)
{
	RECT Rect;
	GetWindowRect(m_Window, &Rect);

	return ret = Rect.top;
}

long ExtObject::eGetClientX(LPVAL theParams, ExpReturn& ret)
{
	POINT pt;
	pt.x = 0;
	pt.y = 0;
	ClientToScreen(m_Window, &pt);

	return ret = pt.x;
}

long ExtObject::eGetClientY(LPVAL theParams, ExpReturn& ret)
{
	POINT pt;
	pt.x = 0;
	pt.y = 0;
	ClientToScreen(m_Window, &pt);

	return ret = pt.y;
}

long ExtObject::eGetClientWidth(LPVAL theParams, ExpReturn& ret)
{
	RECT Rect;
	GetClientRect(m_Window, &Rect);
	return ret = Rect.right - Rect.left;
}

long ExtObject::eGetClientHeight(LPVAL theParams, ExpReturn& ret)
{
	RECT Rect;
	GetClientRect(m_Window, &Rect);
	return ret = Rect.bottom - Rect.top;
}


long ExtObject::eGetWidth(LPVAL theParams, ExpReturn& ret)
{
	RECT Rect;
	GetWindowRect(m_Window, &Rect);


	return ret = Rect.right - Rect.left;
}

long ExtObject::eGetHeight(LPVAL theParams, ExpReturn& ret)
{
	RECT Rect;
	GetWindowRect(m_Window, &Rect);

	return ret = Rect.bottom - Rect.top;
}

#endif // #ifdef RUN_ONLY

//////////////////////////////////////////////////////////////////////////////////
// ACE tables
//////////////////////////////////////////////////////////////////////////////////

void DefineACES(MicroAceTime* at)
{
	// Actions
	ADDPARAM(PARAM_VALUE, "Handle", "Window handle to attach to");
	ADDACT("Attach", "Window", "Attach to handle %0", &ExtObject::aAttach, "Attach", 0);

	ADDACT("Close window", "Window", "Close window", &ExtObject::aCloseWindow, "Close", 0);

	ADDACT("Hide", "Window", "Hide window", &ExtObject::aHideWindow, "Hide", 0);
	ADDACT("Show", "Window", "Show window", &ExtObject::aShowWindow, "Show", 0);
	ADDACT("Flash", "Window", "Flash window", &ExtObject::aFlashWindow, "Flash", 0);

	ADDPARAM(PARAM_STRING, "Title", "Window title");
	ADDACT("Set caption", "Window", "Set caption to %0", &ExtObject::aSetWindowTitle, "SetCaption", 0);

	ADDACT("Minimize", "Window", "Minimize window", &ExtObject::aMinimizeWindow, "Minimize", 0);
	ADDACT("Maximize", "Window", "Maximize window", &ExtObject::aMaximizeWindow, "Maximize", 0);
	ADDACT("Restore", "Window", "Restore window", &ExtObject::aRestoreWindow, "Restore", 0);

	ADDPARAMCOMBO("Always on top", "Window appears over all other windows", "Off|On");
	ADDACT("Set always on top", "Window", "Set always on top %0", &ExtObject::aAlwaysOnTop, "AlwaysOnTop", 0);

	ADDACT("Bring to top", "Window", "Bring window to top", &ExtObject::aBringToTop, "BringToTop", 0);

	ADDPARAM(PARAM_VALUE, "X", "X position in pixels relative to the screen");
	ADDACT("Set X position", "Size & Position", "Set X to %0", &ExtObject::aSetX, "SetX", 0);

	ADDPARAM(PARAM_VALUE, "Y", "Y position in pixels relative to the screen");
	ADDACT("Set Y position", "Size & Position", "Set Y to %0", &ExtObject::aSetY, "SetY", 0);

	ADDPARAM(PARAM_VALUE, "X", "X position in pixels relative to the screen");
	ADDPARAM(PARAM_VALUE, "Y", "Y position in pixels relative to the screen");
	ADDACT("Set position", "Size & Position", "Set position to (%0, %1)", &ExtObject::aSetPosition, "SetXY", 0);

	ADDPARAM(PARAM_VALUE, "Height", "Height in pixels");
	ADDACT("Set height", "Size & Position", "Set height to %0", &ExtObject::aSetHeight, "SetHeight", 0);

	ADDPARAM(PARAM_VALUE, "Width", "Width in pixels");
	ADDACT("Set width", "Size & Position", "Set width to %0", &ExtObject::aSetWidth, "SetWidth", 0);

	ADDPARAM(PARAM_VALUE, "Width", "Width in pixels");
	ADDPARAM(PARAM_VALUE, "Height", "Height in pixels");
	ADDACT("Set size", "Size & Position", "Set size to (%0, %1)", &ExtObject::aSetSize, "SetSize", 0);

	ADDPARAM(PARAM_VALUE, "Client height", "Height of the client's area, the window is supposed to enclose, in pixels");
	ADDACT("Set client height", "Size & Position", "Set client height to %0", &ExtObject::aSetClientHeight, "SetClientHeight", 0);

	ADDPARAM(PARAM_VALUE, "Client width", "Width of the client's area, the window is supposed to enclose, in pixels");
	ADDACT("Set client width", "Size & Position", "Set client width to %0", &ExtObject::aSetClientWidth, "SetClientWidth", 0);

	ADDPARAM(PARAM_VALUE, "Client width", "Width of the client's area, the window is supposed to enclose, in pixels");
	ADDPARAM(PARAM_VALUE, "Client height", "Height of the client's area, the window is supposed to enclose, in pixels");
	ADDACT("Set client size", "Size & Position", "Set client size to (%0, %1)", &ExtObject::aSetClientSize, "SetClientSize", 0);
	
	ADDACT("Attach to current window", "Window", "Attach to current application window", &ExtObject::aAttachToCurrent, "AttachCurrent", 0);

	// Expressions
	ADDPARAM(PARAM_STRING, "Title", "Window title to find");
	ADDEXP("Find window", "Window", "FindWindow", &ExtObject::eFindWindow, RETURN_VALUE);
	ADDEXP("Get application window", "Window", "AppWindow", &ExtObject::eGetApplicationWindow, RETURN_VALUE);

	ADDEXP("Get window titles", "Array", "WindowTitles", &ExtObject::eGetWindows, RETURN_ARRAY);

	ADDEXP("Get caption", "Window", "Caption", &ExtObject::eGetWindowTitle, RETURN_STRING);

	ADDEXP("Get X", "Size & Position", "X", &ExtObject::eGetX, RETURN_VALUE);
	ADDEXP("Get Y", "Size & Position", "Y", &ExtObject::eGetY, RETURN_VALUE);
	ADDEXP("Get Width", "Size & Position", "Width", &ExtObject::eGetWidth, RETURN_VALUE);
	ADDEXP("Get Height", "Size & Position", "Height", &ExtObject::eGetHeight, RETURN_VALUE);



	ADDEXP("Get Client X", "Size & Position", "ClientX", &ExtObject::eGetClientX, RETURN_VALUE);
	ADDEXP("Get Client Y", "Size & Position", "ClientY", &ExtObject::eGetClientY, RETURN_VALUE);
	ADDEXP("Get Client Width", "Size & Position", "ClientWidth", &ExtObject::eGetClientWidth, RETURN_VALUE);
	ADDEXP("Get Client Height", "Size & Position", "ClientHeight", &ExtObject::eGetClientHeight, RETURN_VALUE);
}