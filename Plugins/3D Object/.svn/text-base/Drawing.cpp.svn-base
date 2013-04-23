// Include StdAfx.h
#include "StdAfx.h"

#ifdef RUN_ONLY

/*
// Project the mouse cursor from screen space to object space
void ExtObject::ProjectScreenToWorld(D3DXVECTOR3* pOut, float screenX, float screenY, float worldZ)
{
	D3DXVECTOR3 lineBegin, lineEnd;

	// Unproject the near and far points given by the screen X,Y coords
	D3DXVECTOR3 screenSpace(screenX, screenY, 0.0f);
	D3DXVec3Unproject(&lineBegin, &screenSpace, &viewport, &projection_matrix, &view_matrix, &worldMatrix);
	screenSpace.z = 1.0f;
	D3DXVec3Unproject(&lineEnd, &screenSpace, &viewport, &projection_matrix, &view_matrix, &worldMatrix);

	// Using a plane intersection, we can determine the object space coordinates of the screen space coords
	// at a certain Z depth, intersecting the line given above.
	orig.z = worldZ;
	D3DXPlaneFromPointNormal(&plane, &orig, &normal);
	D3DXPlaneIntersectLine(pOut, &plane, &lineBegin, &lineEnd);
}
*/

/////////////////////////////
// RUNTIME drawing
// Draw: Construct calls this when it wants you to draw your object.  You can
// leave it blank if your extension doesn't draw anything.

void ExtObject::transform_vertices( vector<cr::point3d>& verts, bool screen)
{
	cr::point3d objpos(info.x, info.y, z);

	cr_float sin_a, sin_b, sin_c;
	cr_float cos_a, cos_b, cos_c;

	cr_float temp;
	cr_float zz;

	cr::sincosf(-yaw, &sin_a, &cos_a);
	cr::sincosf(-pitch, &sin_b, &cos_b);
	cr::sincosf(cr::to_radians(info.angle), &sin_c, &cos_c);

	vector<cr::point3d>::iterator v = verts.begin();
	for( ; v != verts.end(); v++)
	{
		cr::point vertexpos(v->x * info.w * scale, v->y * info.h * scale);
		zz = v->z * depth * scale;

		temp = (vertexpos.x * cos_a) - (zz * sin_a);
		zz = (zz * cos_a) + (vertexpos.x * sin_a);
		vertexpos.x = temp;

		temp = (zz * cos_b) - (vertexpos.y * sin_b);
		vertexpos.y = (vertexpos.y * cos_b) + (zz * sin_b);
		zz = temp;

		vertexpos.rotate(cr::to_radians(info.angle));
		cr::point3d vertexpos3d(-vertexpos.x, -vertexpos.y, zz);

		cr::point3d vertex = objpos + vertexpos3d;

		if(screen)
		{
			vertex.x -= pLayout->scrollX;
			vertex.y -= pLayout->scrollY;
		}
		
		*v = vertex;


	}
}


cr::point3d makepoint3d( xyz pos)
{
	return cr::point3d(pos.x, pos.y, pos.z);
}


void ExtObject::Draw()
{
	// Enable the Z stencil
	cr::renderstate_value old_zbuffer_state = renderer->GetRenderState(cr::rs_zbuffer_enabled);
	cr::samplerstate_value oldU = renderer->GetSamplerState(cr::ss_addressu);
	cr::samplerstate_value oldV = renderer->GetSamplerState(cr::ss_addressv);

	renderer->SetRenderState(cr::rs_zbuffer_enabled, cr::rsv_enabled );

	renderer->SetSamplerState(cr::ss_addressu, cr::ssv_wrap);
	renderer->SetSamplerState(cr::ss_addressv, cr::ssv_wrap);

	renderer->SetTexture(info.curTexture);

	if(needToMakeBuffers)
	{
		needToMakeBuffers = false;
		for(vector<obj>::iterator o = myobject.objs.begin(); o != myobject.objs.end(); o++)
		{
			int vertexCount = o->points.size();
			vector<cr::point3d> v;
			vector<cr::point> vt;
			vector<cr::color> c;

			// Now add all the vertices to direct x
			vector<obj_face_point>::iterator p = o->points.begin();
			for(; p!= o->points.end(); p++)
			{
				v.push_back(*p->v);
				vt.push_back(*p->vt);
				c.push_back(cr::opaque_white);
			}

			o->vertexBuffer = (void*)renderer->CreateVertexBatch(&*v.begin(), &*vt.begin(), &*c.begin(), vertexCount);
			
			// Now add all the indexes
			vector<unsigned short> indices;
			
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
						indices.push_back((unsigned short)*i);
					}
				}
			}

			o->indexBuffer = (void*)renderer->CreateIndexBatch(&*indices.begin(), indices.size());
		}
		//-----------------------
	}



	// Calculate scaling matrix
	D3DXMATRIX scaleMatrix;
	D3DXMatrixScaling(&scaleMatrix, -info.w * scale, -info.h * scale, depth * scale);

	D3DXMATRIX rotMatrixX, rotMatrixY, rotMatrixZ, rotMatrix;
	D3DXMatrixRotationY(&rotMatrixY, -yaw);	
	D3DXMatrixRotationX(&rotMatrixX, -pitch);
	D3DXMatrixRotationZ(&rotMatrixZ, cr::to_radians(info.angle + roll));		
	D3DXMatrixMultiply(&rotMatrix, &rotMatrixY, &rotMatrixX);
	D3DXMatrixMultiply(&rotMatrix, &rotMatrix, &rotMatrixZ);

	// Multiply the translation, rotation and scaling matrices together to the world matrix
	D3DXMATRIX transMatrix;
	D3DXMatrixTranslation(&transMatrix, info.x - pLayout->scrollX, info.y - pLayout->scrollY,  z);

	D3DXMATRIX worldMatrix;
	D3DXMatrixMultiply(&worldMatrix,  &scaleMatrix, &rotMatrix);
	D3DXMatrixMultiply(&worldMatrix, &worldMatrix, &transMatrix );

	for(vector<obj>::iterator o = myobject.objs.begin(); o != myobject.objs.end(); o++)
	{
		int vertex_count = o->points.size();
		int index_count = o->number_of_indexes;

		renderer->DrawIndexedVertexTriangles((int)o->vertexBuffer, (int)o->indexBuffer, vertex_count, index_count, (float*)&worldMatrix);
	}

	renderer->SetRenderState(cr::rs_zbuffer_enabled, old_zbuffer_state);
	renderer->SetSamplerState(cr::ss_addressu, oldU);
	renderer->SetSamplerState(cr::ss_addressv, oldV);
}

#else // RUN_ONLY

// Project the mouse cursor from screen space to object space
void EditExt::ProjectScreenToWorld(D3DXVECTOR3* pOut, float screenX, float screenY, float worldZ)
{
	D3DXVECTOR3 lineBegin, lineEnd;

	// Unproject the near and far points given by the screen X,Y coords
	D3DXVECTOR3 screenSpace(screenX, screenY, 0.0f);
	D3DXVec3Unproject(&lineBegin, &screenSpace, &viewport, &projection_matrix, &view_matrix, &worldMatrix);
	screenSpace.z = 1.0f;
	D3DXVec3Unproject(&lineEnd, &screenSpace, &viewport, &projection_matrix, &view_matrix, &worldMatrix);

	// Using a plane intersection, we can determine the object space coordinates of the screen space coords
	// at a certain Z depth, intersecting the line given above.
	orig.z = worldZ;
	D3DXPlaneFromPointNormal(&plane, &orig, &normal);
	D3DXPlaneIntersectLine(pOut, &plane, &lineBegin, &lineEnd);
}

/////////////////////////////
// EDITTIME drawing
// Draw your object in the frame editor.
void EditExt::Draw()
{
	float ox = pInfo->objectX - (pInfo->objectWidth / 2);
	float oy = pInfo->objectY - (pInfo->objectHeight / 2);
	CRect objectRec(ox, oy, ox + pInfo->objectWidth, oy + pInfo->objectHeight);

	float scaledZ = z / (pInfo->pEditTime->GetEyeDistance3d() / 8.0);
	float scaledHeight = depth / (pInfo->pEditTime->GetEyeDistance3d() / 8.0);

	pEditTime->SetTexture(pEditTime->AddImageAsTexture(imgTexture));

	pEditTime->EndBatch();

	if (pDevice == NULL)
		D3DInit();

	HRESULT hr;

	// Ensure Z buffer created
	CreateZBuffer();

	// Clear the Z buffer if necessary
	unsigned int curFrame = pEditTime->GetFrameCount();
	unsigned int lastFrame = (unsigned int)pEditTime->GetLayoutKey("zbufferclearframe");

	if (lastFrame < curFrame) {
		hr = pDevice->Clear(0, NULL, D3DCLEAR_ZBUFFER, 0, 1.0f, 0);

		if (FAILED(hr)) MessageBox(NULL, "Failed to clear Z buffer", "3D Box object", MB_OK | MB_ICONHAND);

		pEditTime->SetLayoutKey("zbufferclearframe", (void*)curFrame, false);
	}

	pDevice->SetRenderState(D3DRS_LIGHTING,FALSE);
	//pDevice->SetRenderState(D3DRS_CULLMODE,D3DCULL_CCW);  We cant change the culling because its unchangeable at runtime because Ashley didn't add it as a cr render state, and probably wont add it coz that would be feature creeping
	pDevice->SetTransform(D3DTS_VIEW, &view_matrix);
	pDevice->SetTransform(D3DTS_PROJECTION, &projection_matrix);
	pDevice->SetSamplerState(0, D3DSAMP_ADDRESSU, D3DTADDRESS_WRAP);
	pDevice->SetSamplerState(0, D3DSAMP_ADDRESSV, D3DTADDRESS_WRAP);

	pDevice->GetViewport(&viewport);
	D3DXMatrixIdentity(&worldMatrix);

	// Enable the Z stencil
	if (true) {
		hr = pDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_TRUE);

		if (FAILED(hr)) MessageBox(NULL, "Failed to enable Z buffer", "3D Box object", MB_OK | MB_ICONHAND);
	}

	if (!drawInit) {

		// Calculate world units per pixel
		D3DXVECTOR3 pt1, pt2;
		ProjectScreenToWorld(&pt1, 0.0f, 0.0f, 0.0f);
		ProjectScreenToWorld(&pt2, 1.0f, 1.0f, 0.0f);
		worldPxX = pt1.x - pt2.x;
		worldPxY = pt1.y - pt2.y;
		if (worldPxX < 0) worldPxX = -worldPxX;
		if (worldPxY < 0) worldPxY = -worldPxY;

		drawInit = true;
	}

	// Calculate scaling matrix
	D3DXMatrixScaling(&scaleMatrix, pInfo->objectWidth * worldPxX * scale, pInfo->objectHeight * worldPxY * scale, scaledHeight * pEditTime->GetZoom() * scale);

	// Calculate the X and Y world coords to place the box at.
	D3DXVECTOR3 objectSpace;
	float objX = pInfo->objectX;
	float objY = pInfo->objectY;
	pEditTime->TranslateFrameToScreenCoords(objX, objY);
	ProjectScreenToWorld(&objectSpace, objX, objY, 0.0f);

	//D3DXMatrixRotationYawPitchRoll(&rotMatrix, RADIANS(yaw), RADIANS(pitch), RADIANS(pInfo->objectAngle) + RADIANS(roll));
	D3DXMATRIX rotMatrixX, rotMatrixY, rotMatrixZ, rotMatrix;
	D3DXMatrixRotationY(&rotMatrixY, -RADIANS(yaw));	
	D3DXMatrixRotationX(&rotMatrixX, -RADIANS(pitch));
	D3DXMatrixRotationZ(&rotMatrixZ, cr::to_radians(pInfo->objectAngle + roll));		
	D3DXMatrixMultiply(&rotMatrix, &rotMatrixY, &rotMatrixX);
	D3DXMatrixMultiply(&rotMatrix, &rotMatrix, &rotMatrixZ);

	// Multiply the translation, rotation and scaling matrices together to the world matrix
	D3DXMatrixTranslation(&transMatrix, objectSpace.x, objectSpace.y, scaledZ *pEditTime->GetZoom());

	D3DXMatrixMultiply(&worldMatrix,  &scaleMatrix, &rotMatrix);
	D3DXMatrixMultiply(&worldMatrix, &worldMatrix, &transMatrix );

	// Use our translated, rotated and scaled matrix as the world matrix
	hr = pDevice->SetTransform(D3DTS_WORLD, &worldMatrix);

	if (FAILED(hr)) MessageBox(NULL, "Failed to set transform", "3D Box object", MB_OK | MB_ICONHAND);

	// Use our vertex stream

	for(vector<obj>::iterator o = myobject.objs.begin(); o != myobject.objs.end(); o++)
	{
		IDirect3DVertexBuffer9* pVertices = (IDirect3DVertexBuffer9*)o->vertexBuffer;
		IDirect3DIndexBuffer9* pIndexes= (IDirect3DIndexBuffer9*)o->indexBuffer;

		if(pVertices && pIndexes)
		{
			hr = pDevice->SetStreamSource(0, pVertices, 0, sizeof(TLVERTEX));
			if (FAILED(hr)) MessageBox(NULL, "Failed to set stream source", "3D Object", MB_OK | MB_ICONHAND);

			hr = pDevice->SetIndices(pIndexes);
			if (FAILED(hr)) MessageBox(NULL, "Failed to set indices", "3D Object", MB_OK | MB_ICONHAND);

			pDevice->DrawIndexedPrimitive(D3DPT_TRIANGLELIST, //PrimitiveType
										   0,0,
										   o->points.size(), 0, o->number_of_indexes / 3);      // Each face is 2 triangles
		}

	}

	// Disable the Z stencil
	hr = pDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_FALSE);
	if (FAILED(hr)) MessageBox(NULL, "Failed to disable Z buffer", "3D Box object", MB_OK | MB_ICONHAND);

	// Restore 2D mode
	pEditTime->SetTexture(-1);
	pEditTime->EndBatch();

	pEditTime->Restore2DMode();

	// Draw a 2D shadow to show where the box lies in the 2D layout
	float ow = pInfo->objectWidth;
	float oh = pInfo->objectHeight;

	double HotSpotAngle = atan2(oh, ow);
	double HotSpotDist = sqrt(ow*ow + oh*oh) / 2;

	float dx = HotSpotDist * cos(RADIANS(pInfo->objectAngle) + HotSpotAngle);
	float dy = HotSpotDist * sin(RADIANS(pInfo->objectAngle) + HotSpotAngle);

	
	pEditTime->Blitrc(pInfo->objectX - dx, pInfo->objectY - dy, ow, oh, pInfo->objectAngle, 0x40404040);
}


#endif // RUN_ONLY