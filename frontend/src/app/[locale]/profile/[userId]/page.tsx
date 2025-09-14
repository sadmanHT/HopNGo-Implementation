'use client';

import React from 'react';
import { ProfilePage } from '../../../../components/social/ProfilePage';
import { useParams } from 'next/navigation';

interface ProfilePageProps {
  params: Promise<{
    userId: string;
    locale: string;
  }>;
}

export default async function UserProfilePage({ params }: ProfilePageProps) {
  const { userId } = await params;

  return (
    <div className="container mx-auto px-4 py-8">
      <ProfilePage userId={userId} />
    </div>
  );
}